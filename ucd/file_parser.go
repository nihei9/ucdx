package ucd

import (
	"bufio"
	"encoding/binary"
	"encoding/hex"
	"fmt"
	"io"
	"regexp"
	"strings"
)

type CodePointRange uint64

func newCodePointRange(from rune, to rune) CodePointRange {
	return CodePointRange(uint64(from)<<32 | uint64(to))
}

func (r CodePointRange) String() string {
	from, to := r.Range()
	if from == to {
		return fmt.Sprintf("%X", from)
	}
	return fmt.Sprintf("%X..%X", from, to)
}

func (r CodePointRange) Range() (from rune, to rune) {
	return rune(r >> 32), rune(r & 0x00000000ffffffff)
}

type field string

func (f field) codePointRange() CodePointRange {
	var from, to rune
	var err error
	cp := reCodePointRange.FindStringSubmatch(string(f))
	from, err = decodeHexToRune(cp[1])
	if err != nil {
		panic(err)
	}
	if cp[2] != "" {
		to, err = decodeHexToRune(cp[2])
		if err != nil {
			panic(err)
		}
	} else {
		to = from
	}
	return newCodePointRange(from, to)
}

func decodeHexToRune(hexCodePoint string) (rune, error) {
	h := hexCodePoint
	if len(h)%2 != 0 {
		h = "0" + h
	}
	b, err := hex.DecodeString(h)
	if err != nil {
		return 0, err
	}
	l := len(b)
	for i := 0; i < 4-l; i++ {
		b = append([]byte{0}, b...)
	}
	n := binary.BigEndian.Uint32(b)
	return rune(n), nil
}

func (f field) symbol() string {
	return string(f)
}

var (
	reLine           = regexp.MustCompile(`^\s*(.*?)\s*(#.*)?$`)
	reCodePointRange = regexp.MustCompile(`^([[:xdigit:]]+)(?:..([[:xdigit:]]+))?$`)

	specialCommentPrefix = "# @missing:"
)

type File[R any, D any] struct {
	Records  []R
	Defaults []D
}

type FileParser[R any, D any] interface {
	parseFields([]field) R
	parseDefaultFields([]field) D
}

// Parse parses data files of Unicode Character Database (UCD).
// Specifically, it has the following two functions:
// - Converts each line of the data files into a slice of fields.
// - Recognizes specially-formatted comments starting `@missing` and generates a slice of fields.
//
// However, for practical purposes, each field needs to be analyzed more specifically.
// For instance, in UnicodeData.txt, the first field represents a range of code points,
// so it needs to be recognized as a hexadecimal string.
// You can perform more specific parsing for each file by implementing a dedicated parser that wraps this parser.
//
// https://www.unicode.org/reports/tr44/#Format_Conventions
func Parse[R any, D any](r io.Reader, fp FileParser[R, D]) (*File[R, D], error) {
	fieldBuf := make([]field, 50)
	defaultFieldBuf := make([]field, 50)
	records := make([]R, 0, 5000)
	defaults := make([]D, 0, 5000)
	scanner := bufio.NewScanner(r)
	for scanner.Scan() {
		fields, defaultFields := parseLine(fieldBuf, defaultFieldBuf, scanner.Text())
		if fields != nil {
			records = append(records, fp.parseFields(fields))
		}
		if defaultFields != nil {
			defaults = append(defaults, fp.parseDefaultFields(defaultFields))
		}
	}
	if err := scanner.Err(); err != nil {
		return nil, err
	}
	return &File[R, D]{
		Records:  records,
		Defaults: defaults,
	}, nil
}

func parseLine(fieldBuf []field, defaultBuf []field, src string) (fields []field, defaultFields []field) {
	ms := reLine.FindStringSubmatch(src)
	mFields := ms[1]
	mComment := ms[2]
	if mFields != "" {
		fields = splitFields(fieldBuf, mFields)
	}
	if strings.HasPrefix(mComment, specialCommentPrefix) {
		defaultFields = splitFields(defaultBuf, strings.Replace(mComment, specialCommentPrefix, "", -1))
	}
	return
}

func splitFields(buf []field, src string) []field {
	n := 0
	for _, f := range strings.Split(src, ";") {
		buf[n] = field(strings.TrimSpace(f))
		n++
	}
	return buf[:n]
}
