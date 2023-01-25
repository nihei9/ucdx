package ucd

import "fmt"

const unicodeVersion = "15.0.0"

func NewUCDFileURL(ucdFileName string) string {
	return fmt.Sprintf("https://www.unicode.org/Public/%v/ucd/%v", unicodeVersion, ucdFileName)
}
