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
	err := gen()
	if err != nil {
		fmt.Fprintf(os.Stderr, "%v\n", err)
		os.Exit(1)
	}
}

func gen() error {
	{
		resp, err := http.Get("https://www.unicode.org/Public/13.0.0/ucd/PropertyValueAliases.txt")
		if err != nil {
			return err
		}
		defer resp.Body.Close()
		f, err := ucd.Parse(resp.Body, ucd.NewPropertyValueAliasesParser())
		if err != nil {
			return err
		}
		cachePath, err := xdg.CacheFile("ucdx/PropertyValueAliases.gob")
		if err != nil {
			return err
		}
		out, err := os.OpenFile(cachePath, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0644)
		if err != nil {
			return err
		}
		defer out.Close()
		gob.NewEncoder(out).Encode(f)
	}
	{
		resp, err := http.Get("https://www.unicode.org/Public/13.0.0/ucd/UnicodeData.txt")
		if err != nil {
			return err
		}
		defer resp.Body.Close()
		f, err := ucd.Parse(resp.Body, ucd.NewUnicodeDataParser())
		if err != nil {
			return err
		}
		cachePath, err := xdg.CacheFile("ucdx/UnicodeData.gob")
		if err != nil {
			return err
		}
		out, err := os.OpenFile(cachePath, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0644)
		if err != nil {
			return err
		}
		defer out.Close()
		gob.NewEncoder(out).Encode(f)
	}
	// for i, rec := range propValAliases.Records {
	// 	fmt.Printf("%5v: %v; %v; %v", i, rec.PropertyName, rec.ShortName, rec.LongName)
	// 	for _, o := range rec.Others {
	// 		fmt.Printf("; %v", o)
	// 	}
	// 	fmt.Print("\n")
	// }
	// for i, rec := range propValAliases.Defaults {
	// 	fmt.Printf("%5v: %v; %v; %v\n", i, rec.CodePoint, rec.PropertyName, rec.ValueName)
	// }
	// for i, rec := range unicodeData.Records {
	// 	fmt.Printf("%5v: %v; gc=%v\n", i, rec.CodePoint, rec.GeneralCategory)
	// }
	return nil
}
