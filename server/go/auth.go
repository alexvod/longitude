package auth

import (
	"log"
	"http"
	"flag"
	authData "./proto/auth_data.pb"
        proto "goprotobuf.googlecode.com/hg/proto"
	"./util"
)

var configFile *string = flag.String("auth_file", "", "Path to file with authentication config")

var loginHtml string = string(util.ReadFileOrDie("html/login.html"))

type Validator interface {
	// Validate an HTTP request. Returns true if ok. Respond with error
	// message to client if there is an error.
	Validate(w http.ResponseWriter, r *http.Request) (bool, *string)

	LoginHandler(w http.ResponseWriter, r *http.Request)
}

func readAuthConfig(path string) *authData.AuthData {
	asciiConfig := util.ReadFileOrDie(path)
	protoConfig := &authData.AuthData{};
	
	err := proto.UnmarshalText(string(asciiConfig), protoConfig)
	if err != nil {
		log.Fatal("Cannot parse config: ", err)
	}
	return protoConfig
}

type validatorImpl struct {
	tokenMap map[string]string
}

func getCookieValue(cookie []*http.Cookie, name string) *string {
	for i := range cookie {
		c := cookie[i]
		if c.Name == name {
			return &c.Value
		}
	}
	return nil
}

func (authData* validatorImpl) Validate(w http.ResponseWriter, r *http.Request) (bool, *string) {
	client := getCookieValue(r.Cookies(), "client")
	if client == nil {
		http.Error(w, "Missing client cookie", http.StatusBadRequest)
		return false, nil
	}
	token := getCookieValue(r.Cookies(), "token")
	if token == nil {
		http.Error(w, "Missing token cookie", http.StatusBadRequest)
		return false, nil
	}
	existingToken, found := authData.tokenMap[*client]
	if !found {
		log.Printf("Unknown client " + *client)
		http.Error(w, "Invalid auth token", http.StatusForbidden)
		return false, nil
	}
	if *token != existingToken {
		log.Printf("Invalid token: %s != %s", *token, existingToken)
		http.Error(w, "Invalid auth token", http.StatusForbidden)
		return false, nil
	}
	return true, client
}

func (authData* validatorImpl) LoginHandler(w http.ResponseWriter, r *http.Request) () {
	client := r.FormValue("client")
	token := r.FormValue("token")

	if (client == "" || token == "") {
		w.Write([]byte(loginHtml))
		return
	}

	existingToken, found := authData.tokenMap[client]
	if !found {
		http.Error(w, "Invalid auth token", http.StatusForbidden)
		return
	}
	if token != existingToken {
		log.Printf("Invalid token: %s != %s", token, existingToken)
		http.Error(w, "Invalid auth token", http.StatusForbidden)
		return
	}

	// Set cookies.
	clientCookie := &http.Cookie{Name: "client", Value: client, Secure: true}
	http.SetCookie(w, clientCookie)

	tokenCookie := &http.Cookie{Name: "token", Value: token, Secure: true}
	http.SetCookie(w, tokenCookie)

	w.Write([]byte("Login successful"))
}

func NewValidator(configFile string) Validator {
	authConfig := readAuthConfig(configFile)
	tokenMap := map[string]string{};
	for i := 0; i < len(authConfig.User); i++ {
		tokenMap[*authConfig.User[i].Name] = *authConfig.User[i].Token
	}
	return &validatorImpl{tokenMap}
}	

func NewDefaultValidator() Validator {
	return NewValidator(*configFile)
}
