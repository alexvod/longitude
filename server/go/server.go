package main

import (
	"container/list"
	"flag"
	"fmt"
	"http"
	"io/ioutil"
	"log"
	"regexp"
	"strings"
	"time"
        proto "goprotobuf.googlecode.com/hg/proto"
	locationProto "./proto/location.pb"
	"./util"
	"./query"
	"./auth"
)

var port *int = flag.Int("port", 46940, "Port to listen on")

var indexHtml string = string(util.ReadFileOrDie("html/index.html"))

const kDefaultPollTimeoutSec = 60 * 60

type Location struct {
	lat float32
	lng float32
	accuracy float32
	timestamp int64
}

type ManagerRequest interface {
	locationManagerRequest()
}

type GetLocationsRequest struct {
	out chan *map[string]Location
}

func (x *GetLocationsRequest) locationManagerRequest() {}

type UpdateLocationRequest struct {
	id string
	location *Location
	out chan bool
}

func (x *UpdateLocationRequest) locationManagerRequest() {}

type WaitForUpdatesRequest struct {
	out chan *map[string]Location
}

func (x *WaitForUpdatesRequest) locationManagerRequest() {}

func ValidateCoords(lat float32, lng float32, accuracy float32) *string {
	if (lat < -90) || (lat > 90) {
		msg := fmt.Sprintf("Bad latitude: %.3f", lat)
		return &msg
	}
	if (lat < -180) || (lat > 180) {
		msg := fmt.Sprintf("Bad longitude: %.3f", lng)
		return &msg
	}
	if (accuracy < 0) || (accuracy > 10000) {
		msg := fmt.Sprintf("Bad accuracy: %.3f", accuracy)
		return &msg
	}
	return nil
}

func CopyMap(src map[string]Location, dst map[string]Location) {
	for key, value := range src {
		dst[key] = value
	}
}

func Manager(in chan ManagerRequest) {
	locations := map[string]Location{}
	waiters := list.New()
	for {
		req := <- in
		switch t := req.(type) {
		case *GetLocationsRequest:
			getLocReq := req.(*GetLocationsRequest)
			newLocations := map[string]Location{}
			CopyMap(locations, newLocations)
			getLocReq.out <- &newLocations
		case *UpdateLocationRequest:
			updateReq := req.(*UpdateLocationRequest)
			locations[updateReq.id] = *updateReq.location
			updateReq.out <- true
			newLocations := map[string]Location{}
			CopyMap(locations, newLocations)
			for e := waiters.Front(); e != nil; e = e.Next() {
				waiter := e.Value.(chan *map[string]Location)
				waiter <- &newLocations
			}
			waiters = list.New()
		case *WaitForUpdatesRequest:
			pollReq := req.(*WaitForUpdatesRequest)
			waiters.PushBack(pollReq.out)
		default:
			panic(fmt.Sprintf("Unknown request type %T", t))
		}
	}
}

func JavascriptHandler(w http.ResponseWriter, r *http.Request) {
	params, _ := http.ParseQuery(r.URL.RawQuery)
	name, err := query.GetQueryParam(params, "name")
	if err != nil {
		http.Error(w, *err, http.StatusBadRequest)
		return
	}
	reg := regexp.MustCompile("[^a-z_]")
	if reg.MatchString(*name) {
		http.Error(w, "Bad filename", http.StatusBadRequest)
		return
	}
	data, osErr := ioutil.ReadFile(*name + ".js")
	if osErr != nil {
		http.Error(w, "File not found", http.StatusBadRequest)
		return
	}
	w.Header().Add("Content-type", "text/javascript")
	w.Write(data)
}

func GetAllLocations(manager chan ManagerRequest) *map[string]Location {
	out := make(chan *map[string]Location, 1)
	manager <- &GetLocationsRequest{out}
	locations := <- out
	return locations
}

func PrintLocationsAsJson(locations map[string]Location) string {
	result := ""
	for name, location := range locations {
		// TODO: proper escaping here
		if result != "" {
			result = result + ","
		}
		result += fmt.Sprintf("'%s': {'lat': %.5f, 'lng': %.5f, 'accuracy': %.5f, 'time': %d}",
			name, location.lat, location.lng, location.accuracy, location.timestamp)
	}
	result = "{" + result + "}"
	return result
}
	
func PrintLocationsAsProto(locations map[string]Location) []byte {
	locProto := &locationProto.LocationInfo{
		make([]*locationProto.Location, len(locations)), nil}
	loc := locProto.Location
	for name, location := range locations {
		loc[0] = &locationProto.Location{
			proto.String(name), proto.Float64(float64(location.lat)), proto.Float64(float64(location.lng)),
			nil, proto.Float64(float64(location.accuracy)), proto.Int64(location.timestamp), nil}
		loc = loc[1:]
	}
	data, _ := proto.Marshal(locProto)
	return data
}

func ShowMainPage(w http.ResponseWriter, r *http.Request, validator auth.Validator, manager chan ManagerRequest) {
	auth_ok, client := validator.Validate(w, r)
	if !auth_ok {
		return
	}
	log.Printf("Got main page request from %s", *client)
	locations := GetAllLocations(manager)
	response := strings.Replace(indexHtml, "@@LOCATIONS@@",
		PrintLocationsAsJson(*locations), -1)
	w.Write([]byte(response))
}

func GetLocations(w http.ResponseWriter, r *http.Request, validator auth.Validator, manager chan ManagerRequest) {
	auth_ok, client := validator.Validate(w, r)
	if !auth_ok {
		return
	}
	log.Printf("Got locations request from %s", *client)

	params, _ := http.ParseQuery(r.URL.RawQuery)
	outFormat, err := query.GetQueryParam(params, "output")
	locations := GetAllLocations(manager)
	
	if err == nil && *outFormat == "proto" {
		w.Header().Add("Content-type", "application/octet-stream")
		w.Write(PrintLocationsAsProto(*locations))
	} else {
		w.Write([]byte(PrintLocationsAsJson(*locations)))
	}
}

func Poll(w http.ResponseWriter, r *http.Request, validator auth.Validator, manager chan ManagerRequest) {
	auth_ok, client := validator.Validate(w, r)
	if !auth_ok {
		return
	}
	
	params, _ := http.ParseQuery(r.URL.RawQuery)
	outFormat, err := query.GetQueryParam(params, "output")
	if err != nil {
		jsonFormat := "json"
		outFormat = &jsonFormat
	}
	timeout, err := query.GetInt32QueryParam(params, "timeout")
	if err != nil {
		timeout = kDefaultPollTimeoutSec
	}
	if timeout <= 0 || timeout > 60 * 60 {
		http.Error(w, fmt.Sprintf("Invalid timeout: %d", timeout), http.StatusBadRequest)
		return
	}
	log.Printf("Got poll request from %s with timeout %d", *client, timeout)

	out := make(chan *map[string]Location, 1)
	manager <- &WaitForUpdatesRequest{out}

	timeoutChan := make(chan bool, 1)
	go func() {
		time.Sleep(int64(timeout) * 1e9)
		timeoutChan <- true
	}()

	select {
	case locations := <- out:
		if *outFormat == "proto" {
			w.Header().Add("Content-type", "application/octet-stream")
			w.Write(PrintLocationsAsProto(*locations))
		} else {
			w.Write([]byte(PrintLocationsAsJson(*locations)))
		}
	case <- timeoutChan:
		log.Printf("Poll request timed out")
		http.Error(w, "Poll request timed out, please try again", http.StatusRequestTimeout)
	}
}

func ParseLocationFromRequest(params map[string][]string) (*Location, *string) {
	lat, err := query.GetFloatQueryParam(params, "lat")
	if err != nil {
		return nil, err
	}
	lng, err := query.GetFloatQueryParam(params, "lng")
	if err != nil {
		return nil, err
	}
	accuracy, err := query.GetFloatQueryParam(params, "acc")
	if err != nil {
		return nil, err
	}
	timestamp, err := query.GetInt64QueryParam(params, "time")
	if err != nil {
		return nil, err
	}

	err = ValidateCoords(lat, lng, accuracy)
	if err != nil {
		return nil, err
	}

	now := time.Seconds() * 1000
	if timestamp > now {
		timestamp = now
	}

	location := &Location{lat, lng, accuracy, timestamp}
	return location, err
}

func UpdateLocation(w http.ResponseWriter, r *http.Request, validator auth.Validator, manager chan ManagerRequest) {
	auth_ok, client := validator.Validate(w, r)
	if !auth_ok {
		return
	}

	params, _ := http.ParseQuery(r.URL.RawQuery)
	location, err := ParseLocationFromRequest(params)
	if err != nil {
		http.Error(w, *err, http.StatusBadRequest)
		return
	}
	log.Printf("Got update request for %s with timestamp %d", *client, location.timestamp)

	// Reject timestamp from future.
	now := time.Seconds() * 1000
	if location.timestamp > now {
		location.timestamp = now
	}

	out := make(chan bool, 1)
	updateRequest := &UpdateLocationRequest{*client, location, out}
	manager <- updateRequest
	_ = <- out
	response := "ok"
	fmt.Fprintf(w, response)
}

func MakeHandler(manager chan ManagerRequest, validator auth.Validator,
	fn func (http.ResponseWriter, *http.Request, auth.Validator, chan ManagerRequest)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		fn(w, r, validator, manager)
	}
}

func main() {
	flag.Parse()
	validator := auth.NewDefaultValidator()
	manager := make(chan ManagerRequest, 1)
	go Manager(manager)
	http.HandleFunc("/", MakeHandler(manager, validator, ShowMainPage))
	http.HandleFunc("/getloc", MakeHandler(manager, validator, GetLocations))
	http.HandleFunc("/update", MakeHandler(manager, validator, UpdateLocation))
	http.HandleFunc("/poll", MakeHandler(manager, validator, Poll))
	http.HandleFunc("/js", JavascriptHandler)
	log.Printf("Listening on HTTPS port %d", *port)
	//err := http.ListenAndServe(fmt.Sprintf(":%d", *port), nil)
	err := http.ListenAndServeTLS(fmt.Sprintf(":%d", *port), "cert.pem", "key.pem", nil)
	if err != nil {
		log.Fatal(err)
	}
}
