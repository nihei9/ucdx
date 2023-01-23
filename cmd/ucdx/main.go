package main

import (
	"encoding/gob"
	"fmt"
	"net/http"
	"os"

	"github.com/adrg/xdg"
	"github.com/nihei9/ucdx/ucd"
	"golang.org/x/sync/errgroup"
)

func main() {
	err := run()
	if err != nil {
		fmt.Fprintf(os.Stderr, "%v\n", err)
		os.Exit(1)
	}
}

func run() error {
	g := &errgroup.Group{}
	g.Go(func() error {
		return cache("PropertyValueAliases.txt", ucd.NewPropertyValueAliasesParser())
	})
	g.Go(func() error {
		return cache("UnicodeData.txt", ucd.NewUnicodeDataParser())
	})
	g.Go(func() error {
		return cache("PropList.txt", ucd.NewPropListParser())
	})
	g.Go(func() error {
		return cache("Scripts.txt", ucd.NewScriptsParser())
	})
	return g.Wait()
}

func cache[R any, D any](ucdFileName string, parser ucd.FileParser[R, D]) error {
	resp, err := http.Get(ucd.NewUCDFileURL(ucdFileName))
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
