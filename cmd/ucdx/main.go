package main

import (
	"fmt"
	"log"
	"net/http"
	"os"

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
	var propValAliases *ucd.File[*ucd.PropertyValueAliasesRecord, *ucd.PropertyValueAliasesDefaultRecord]
	{
		log.Println("Get PropertyValueAliases.txt")
		resp, err := http.Get("https://www.unicode.org/Public/13.0.0/ucd/PropertyValueAliases.txt")
		if err != nil {
			return err
		}
		defer resp.Body.Close()
		log.Println("OK")
		log.Println("Start parse")
		propValAliases, err = ucd.Parse(resp.Body, ucd.NewPropertyValueAliasesParser())
		if err != nil {
			return err
		}
		log.Print("Finished")
	}
	// var unicodeData *ucd.File[*ucd.UnicodeDataRecord, *ucd.UnicodeDataDefaultRecord]
	// {
	// 	resp, err := http.Get("https://www.unicode.org/Public/13.0.0/ucd/UnicodeData.txt")
	// 	if err != nil {
	// 		return err
	// 	}
	// 	defer resp.Body.Close()
	// 	unicodeData, err = ucd.Parse(resp.Body, ucd.NewUnicodeDataParser())
	// 	if err != nil {
	// 		return err
	// 	}
	// }
	for i, rec := range propValAliases.Records {
		fmt.Printf("%5v: %v; %v; %v", i, rec.PropertyName, rec.ShortName, rec.LongName)
		for _, o := range rec.Others {
			fmt.Printf("; %v", o)
		}
		fmt.Print("\n")
	}
	for i, rec := range propValAliases.Defaults {
		fmt.Printf("%5v: %v; %v; %v\n", i, rec.CodePoint, rec.PropertyName, rec.ValueName)
	}
	// for i, rec := range unicodeData.Records {
	// 	fmt.Printf("%5v: %v; gc=%v\n", i, rec.CodePoint, rec.GeneralCategory)
	// }
	return nil
}
