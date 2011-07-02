package query

import "strconv"

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

