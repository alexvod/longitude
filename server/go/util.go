package util

import (
	"log"
	"io/ioutil"
)

func ReadFileOrDie(name string) []byte {
	data, err := ioutil.ReadFile(name)
	if err != nil {
		log.Panicf("Cannot read file %s", name)
	}
	return data
}

