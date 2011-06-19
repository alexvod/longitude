package main

import (
	"flag"
	"fmt"
	"http"
	"io/ioutil"
	"log"
	"regexp"
	"strconv"
	"strings"
	"time"
        proto "goprotobuf.googlecode.com/hg/proto"
	locationProto "./proto/location.pb"
)

var port *int = flag.Int("port", 46940, "Port to listen on")

var indexHtml string = string(ReadFileOrDie("html/index.html"))

func ReadFileOrDie(name string) []byte {
	data, err := ioutil.ReadFile(name)
	if err != nil {
		log.Panicf("Cannot read file %s", name)
	}
	return data
}

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
	locations map[string]Location
	out chan bool
}

func (x *GetLocationsRequest) locationManagerRequest() {}

type UpdateLocationRequest struct {
	id string
	location *Location
	out chan bool
}

func (x *UpdateLocationRequest) locationManagerRequest() {}

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

func Manager(in chan ManagerRequest) {
	locations := map[string]Location{}
	for {
		req := <- in
		switch t := req.(type) {
		case *GetLocationsRequest:
			log.Printf("Got get locations request")
			getLocReq := req.(*GetLocationsRequest)
			for name, location := range locations {
				getLocReq.locations[name] = location
			}
			getLocReq.out <- true
		case *UpdateLocationRequest:
			updateReq := req.(*UpdateLocationRequest)
			log.Printf("Got update request for %s", updateReq.id)
			locations[updateReq.id] = *updateReq.location
			updateReq.out <- true
		default:
			panic(fmt.Sprintf("Unknown request type %T", t))
		}
	}
}

func GetQueryParam(params map[string][]string, name string) (*string, *string) {
	allValues := params[name]
	if len(allValues) == 0 {
		message := "Missing " + name
		return nil, &message
	}
	if len(allValues) > 1 {
		message := "More than one value of " + name
		return nil, &message
	}
	return &allValues[0], nil
}

func GetInt32QueryParam(params map[string][]string, name string) (int, *string) {
	stringValue, err := GetQueryParam(params, name)
	if err != nil {
		return 0, err
	}
	result, convErr := strconv.Atoi(*stringValue)
	if convErr != nil {
		message := "Invalid value for parameter " + name + ", expected integer"
		return 0, &message
	}
	return result, nil
}

func GetInt64QueryParam(params map[string][]string, name string) (int64, *string) {
	stringValue, err := GetQueryParam(params, name)
	if err != nil {
		return 0, err
	}
	result, convErr := strconv.Atoi64(*stringValue)
	if convErr != nil {
		message := "Invalid value for parameter " + name + ", expected integer"
		return 0, &message
	}
	return result, nil
}

func GetFloatQueryParam(params map[string][]string, name string) (float32, *string) {
	stringValue, err := GetQueryParam(params, name)
	if err != nil {
		return 0.0, err
	}
	result, convErr := strconv.Atof32(*stringValue)
	if convErr != nil {
		message := "Invalid value for parameter " + name + ", expected float"
		return 0.0, &message
	}
	return result, nil
}

func JavascriptHandler(w http.ResponseWriter, r *http.Request) {
	params, _ := http.ParseQuery(r.URL.RawQuery)
	name, err := GetQueryParam(params, "name")
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
	w.Write(data)
}

func GetAllLocations(manager chan ManagerRequest) map[string]Location {
	out := make(chan bool)
	locations := map[string]Location{}
	getLocRequest := &GetLocationsRequest{locations, out}
	manager <- getLocRequest
	_ = <- out
	return locations
}

func GetLocationsJson(manager chan ManagerRequest) string {
	locations := GetAllLocations(manager)
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
	
func GetLocationsProto(manager chan ManagerRequest) []byte {
	locations := GetAllLocations(manager)

	locProto := &locationProto.LocationInfo{make([]*locationProto.Location, len(locations)), nil}
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

func ShowMainPage(w http.ResponseWriter, r *http.Request, manager chan ManagerRequest) {
	response := strings.Replace(indexHtml, "@@LOCATIONS@@", GetLocationsJson(manager), -1)
	w.Write([]byte(response))
}

func GetLocations(w http.ResponseWriter, r *http.Request, manager chan ManagerRequest) {
	params, _ := http.ParseQuery(r.URL.RawQuery)
	outFormat, err := GetQueryParam(params, "output")
	if err == nil && *outFormat == "proto" {
		w.Header().Add("Content-type", "application/octet-stream")
		w.Write(GetLocationsProto(manager))
	} else {
		w.Write([]byte(GetLocationsJson(manager)))
	}
}

func ParseLocationFromRequest(params map[string][]string) (*Location, *string) {
	lat, err := GetFloatQueryParam(params, "lat")
	if err != nil {
		return nil, err
	}
	lng, err := GetFloatQueryParam(params, "lng")
	if err != nil {
		return nil, err
	}
	accuracy, err := GetFloatQueryParam(params, "acc")
	if err != nil {
		return nil, err
	}
	timestamp, err := GetInt64QueryParam(params, "time")
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

func UpdateLocation(w http.ResponseWriter, r *http.Request, manager chan ManagerRequest) {
	params, _ := http.ParseQuery(r.URL.RawQuery)
	name, err := GetQueryParam(params, "id")
	if err != nil {
		http.Error(w, *err, http.StatusBadRequest)
		return
	}
	location, err := ParseLocationFromRequest(params)
	if err != nil {
		http.Error(w, *err, http.StatusBadRequest)
		return
	}

	// Reject timestamp from future.
	now := time.Seconds() * 1000
	if location.timestamp > now {
		location.timestamp = now
	}

	out := make(chan bool)
	updateRequest := &UpdateLocationRequest{*name, location, out}
	manager <- updateRequest
	_ = <- out
	response := "ok"
	fmt.Fprintf(w, response)
}

func MakeHandler(manager chan ManagerRequest,
	fn func (http.ResponseWriter, *http.Request, chan ManagerRequest)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		fn(w, r, manager)
	}
}

func main() {
	flag.Parse()
	manager := make(chan ManagerRequest)
	go Manager(manager)
	http.HandleFunc("/", MakeHandler(manager, ShowMainPage))
	http.HandleFunc("/getloc", MakeHandler(manager, GetLocations))
	http.HandleFunc("/update", MakeHandler(manager, UpdateLocation))
	http.HandleFunc("/js", JavascriptHandler)
	log.Printf("Listening on HTTP port %d", *port)
	err := http.ListenAndServe(fmt.Sprintf(":%d", *port), nil)
	if err != nil {
		log.Fatal(err)
	}
}