package ucd

import "fmt"

func NewUCDFileURL(ucdFileName string) string {
	return fmt.Sprintf("https://www.unicode.org/Public/13.0.0/ucd/%v", ucdFileName)
}
