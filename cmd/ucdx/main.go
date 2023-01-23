package main

import (
	"encoding/gob"
	"fmt"
	"net/http"
	"os"

	"github.com/adrg/xdg"
	"github.com/nihei9/ucdx/ucd"
)

func main() {
	err := run()
	if err != nil {
		fmt.Fprintf(os.Stderr, "%v\n", err)
		os.Exit(1)
	}
}

func run() error {
	err := cache("PropertyValueAliases.txt", ucd.NewPropertyValueAliasesParser())
	if err != nil {
		return err
	}
	err = cache("UnicodeData.txt", ucd.NewUnicodeDataParser())
	if err != nil {
		return err
	}
	err = cache("Scripts.txt", ucd.NewScriptsParser())
	if err != nil {
		return err
	}
	return nil
}

func cache[R any, D any](ucdFileName string, parser ucd.FileParser[R, D]) error {
	resp, err := http.Get(fmt.Sprintf("https://www.unicode.org/Public/13.0.0/ucd/%v", ucdFileName))
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	f, err := ucd.Parse(resp.Body, parser)
	if err != nil {
		return err
	}
	cachePath, err := xdg.CacheFile(fmt.Sprintf("ucdx/%v.gob", ucdFileName))
	if err != nil {
		return err
	}
	out, err := os.OpenFile(cachePath, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0644)
	if err != nil {
		return err
	}
	defer out.Close()
	return gob.NewEncoder(out).Encode(f)
}
