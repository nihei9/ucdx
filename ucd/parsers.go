package ucd

type PropertyValueAliasesRecord struct {
	PropertyName string
	ShortName    string
	LongName     string
	Others       []string
}

type PropertyValueAliasesDefaultRecord struct {
	CodePoint    CodePointRange
	PropertyName string
	ValueName    string
}

// ParsePropertyValueAliasesParser parses the PropertyValueAliases.txt.
type PropertyValueAliasesParser struct {
}

func NewPropertyValueAliasesParser() fileParser[*PropertyValueAliasesRecord, *PropertyValueAliasesDefaultRecord] {
	return &PropertyValueAliasesParser{}
}

// https://www.unicode.org/reports/tr44/#Property_Value_Aliases
// > In PropertyValueAliases.txt, the first field contains the abbreviated alias for a Unicode property,
// > the second field specifies an abbreviated symbolic name for a value of that property, and the third
// > field specifies the long symbolic name for that value of that property. These are the preferred
// > aliases. Additional aliases for some property values may be specified in the fourth or subsequent
// > fields.
func (p *PropertyValueAliasesParser) parseFields(fields []field) *PropertyValueAliasesRecord {
	rec := &PropertyValueAliasesRecord{
		PropertyName: fields[0].symbol(),
		ShortName:    fields[1].symbol(),
		LongName:     fields[2].symbol(),
	}
	if len(fields) > 3 {
		others := make([]string, len(fields)-3)
		for i, f := range fields[3:] {
			others[i] = f.symbol()
		}
		rec.Others = others
	}
	return rec
}

// https://www.unicode.org/reports/tr44/#Missing_Conventions
// > @missing lines are also supplied for many properties in the file PropertyValueAliases.txt.
// > ...
// > there are currently two syntactic patterns used for @missing lines, as summarized schematically below:
// >     1. code_point_range; default_prop_val
// >     2. code_point_range; property_name; default_prop_val
// > ...
// > Pattern #2 is used in PropertyValueAliases.txt and in DerivedNormalizationProps.txt, both of which
// > contain values associated with many properties. For example:
// >     # @missing: 0000..10FFFF; NFD_QC; Yes
func (p *PropertyValueAliasesParser) parseDefaultFields(fields []field) *PropertyValueAliasesDefaultRecord {
	return &PropertyValueAliasesDefaultRecord{
		CodePoint:    fields[0].codePointRange(),
		PropertyName: fields[1].symbol(),
		ValueName:    fields[2].symbol(),
	}
}

type UnicodeDataRecord struct {
	CodePoint       CodePointRange
	GeneralCategory string
}

// UnicodeDataParser parses the UnicodeData.txt.
type UnicodeDataParser struct {
}

func NewUnicodeDataParser() fileParser[*UnicodeDataRecord, *UnicodeDataRecord] {
	return &UnicodeDataParser{}
}

func (p *UnicodeDataParser) parseFields(fields []field) *UnicodeDataRecord {
	rec := &UnicodeDataRecord{
		CodePoint:       fields[0].codePointRange(),
		GeneralCategory: fields[2].symbol(),
	}
	return rec
}

func (p *UnicodeDataParser) parseDefaultFields(fields []field) *UnicodeDataRecord {
	return nil
}
