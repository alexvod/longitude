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

type Validator interface {
	// Validate an HTTP request. Returns true if ok. Respond with error
	// message to client if there is an error.
	Validate(w http.ResponseWriter, r *http.Request) (bool, *string)
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
