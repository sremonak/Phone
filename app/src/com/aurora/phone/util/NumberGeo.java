package com.aurora.phone.util;

import java.util.Locale;

/**
 * Offline phone-number intelligence: country, approximate region,
 * carrier (Bangladesh-aware), number type and pretty formatting.
 * Everything here is heuristic and works without network access.
 */
public final class NumberGeo {

    public static final String TYPE_MOBILE = "mobile";
    public static final String TYPE_LANDLINE = "landline";
    public static final String TYPE_TOLLFREE = "tollfree";
    public static final String TYPE_PREMIUM = "premium";
    public static final String TYPE_SHORT = "short";
    public static final String TYPE_INTERNATIONAL = "intl";
    public static final String TYPE_UNKNOWN = "unknown";

    /** Result holder. */
    public static class Info {
        public String raw = "";
        public String pretty = "";
        public String country = "";
        public String iso = "";
        public String dialCode = "";
        public String region = "";
        public String carrier = "";
        public String type = TYPE_UNKNOWN;

        public boolean isMobile() { return TYPE_MOBILE.equals(type); }
        public boolean isShort() { return TYPE_SHORT.equals(type); }
        public boolean isPremium() { return TYPE_PREMIUM.equals(type); }
        public boolean isTollFree() { return TYPE_TOLLFREE.equals(type); }
        public boolean isLandline() { return TYPE_LANDLINE.equals(type); }
        public boolean isInternational() { return TYPE_INTERNATIONAL.equals(type); }
    }

    private NumberGeo() {
    }

    // --- country table: {dialCode, country, iso} ---
    private static final String[][] COUNTRIES = {
        {"880", "Bangladesh", "BD"},
        {"971", "United Arab Emirates", "AE"},
        {"966", "Saudi Arabia", "SA"},
        {"974", "Qatar", "QA"},
        {"973", "Bahrain", "BH"},
        {"965", "Kuwait", "KW"},
        {"968", "Oman", "OM"},
        {"977", "Nepal", "NP"},
        {"972", "Israel", "IL"},
        {"886", "Taiwan", "TW"},
        {"882", "International", "XX"},
        {"881", "International", "XX"},
        {"888", "International", "XX"},
        {"878", "International", "XX"},
        {"852", "Hong Kong", "HK"},
        {"853", "Macau", "MO"},
        {"850", "North Korea", "KP"},
        {"855", "Cambodia", "KH"},
        {"856", "Laos", "LA"},
        {"86", "China", "CN"},
        {"82", "South Korea", "KR"},
        {"81", "Japan", "JP"},
        {"84", "Vietnam", "VN"},
        {"66", "Thailand", "TH"},
        {"65", "Singapore", "SG"},
        {"63", "Philippines", "PH"},
        {"62", "Indonesia", "ID"},
        {"60", "Malaysia", "MY"},
        {"64", "New Zealand", "NZ"},
        {"61", "Australia", "AU"},
        {"673", "Brunei", "BN"},
        {"679", "Fiji", "FJ"},
        {"91", "India", "IN"},
        {"92", "Pakistan", "PK"},
        {"94", "Sri Lanka", "LK"},
        {"93", "Afghanistan", "AF"},
        {"95", "Myanmar", "MM"},
        {"960", "Maldives", "MV"},
        {"967", "Yemen", "YE"},
        {"964", "Iraq", "IQ"},
        {"963", "Syria", "SY"},
        {"962", "Jordan", "JO"},
        {"961", "Lebanon", "LB"},
        {"98", "Iran", "IR"},
        {"90", "Turkey", "TR"},
        {"998", "Uzbekistan", "UZ"},
        {"996", "Kyrgyzstan", "KG"},
        {"995", "Georgia", "GE"},
        {"994", "Azerbaijan", "AZ"},
        {"993", "Turkmenistan", "TM"},
        {"992", "Tajikistan", "TJ"},
        {"991", "International", "XX"},
        {"7", "Russia / Kazakhstan", "RU"},
        {"380", "Ukraine", "UA"},
        {"389", "North Macedonia", "MK"},
        {"386", "Slovenia", "SI"},
        {"385", "Croatia", "HR"},
        {"381", "Serbia", "RS"},
        {"382", "Montenegro", "ME"},
        {"383", "Kosovo", "XK"},
        {"387", "Bosnia and Herzegovina", "BA"},
        {"370", "Lithuania", "LT"},
        {"371", "Latvia", "LV"},
        {"372", "Estonia", "EE"},
        {"374", "Armenia", "AM"},
        {"375", "Belarus", "BY"},
        {"376", "Andorra", "AD"},
        {"377", "Monaco", "MC"},
        {"378", "San Marino", "SM"},
        {"380x", "", ""},
        {"44", "United Kingdom", "GB"},
        {"49", "Germany", "DE"},
        {"33", "France", "FR"},
        {"39", "Italy", "IT"},
        {"34", "Spain", "ES"},
        {"31", "Netherlands", "NL"},
        {"46", "Sweden", "SE"},
        {"47", "Norway", "NO"},
        {"45", "Denmark", "DK"},
        {"41", "Switzerland", "CH"},
        {"43", "Austria", "AT"},
        {"32", "Belgium", "BE"},
        {"351", "Portugal", "PT"},
        {"30", "Greece", "GR"},
        {"48", "Poland", "PL"},
        {"40", "Romania", "RO"},
        {"36", "Hungary", "HU"},
        {"353", "Ireland", "IE"},
        {"358", "Finland", "FI"},
        {"354", "Iceland", "IS"},
        {"352", "Luxembourg", "LU"},
        {"355", "Albania", "AL"},
        {"359", "Bulgaria", "BG"},
        {"357", "Cyprus", "CY"},
        {"356", "Malta", "MT"},
        {"420", "Czechia", "CZ"},
        {"421", "Slovakia", "SK"},
        {"358x", "", ""},
        {"1", "United States / Canada", "US"},
        {"52", "Mexico", "MX"},
        {"51", "Peru", "PE"},
        {"54", "Argentina", "AR"},
        {"55", "Brazil", "BR"},
        {"56", "Chile", "CL"},
        {"57", "Colombia", "CO"},
        {"58", "Venezuela", "VE"},
        {"506", "Costa Rica", "CR"},
        {"502", "Guatemala", "GT"},
        {"503", "El Salvador", "SV"},
        {"504", "Honduras", "HN"},
        {"505", "Nicaragua", "NI"},
        {"507", "Panama", "PA"},
        {"591", "Bolivia", "BO"},
        {"593", "Ecuador", "EC"},
        {"595", "Paraguay", "PY"},
        {"598", "Uruguay", "UY"},
        {"20", "Egypt", "EG"},
        {"27", "South Africa", "ZA"},
        {"212", "Morocco", "MA"},
        {"213", "Algeria", "DZ"},
        {"216", "Tunisia", "TN"},
        {"218", "Libya", "LY"},
        {"220", "Gambia", "GM"},
        {"221", "Senegal", "SN"},
        {"222", "Mauritania", "MR"},
        {"223", "Mali", "ML"},
        {"224", "Guinea", "GN"},
        {"225", "Ivory Coast", "CI"},
        {"226", "Burkina Faso", "BF"},
        {"227", "Niger", "NE"},
        {"228", "Togo", "TG"},
        {"229", "Benin", "BJ"},
        {"230", "Mauritius", "MU"},
        {"231", "Liberia", "LR"},
        {"232", "Sierra Leone", "SL"},
        {"233", "Ghana", "GH"},
        {"234", "Nigeria", "NG"},
        {"235", "Chad", "TD"},
        {"236", "Central African Republic", "CF"},
        {"237", "Cameroon", "CM"},
        {"238", "Cape Verde", "CV"},
        {"239", "Sao Tome and Principe", "ST"},
        {"240", "Equatorial Guinea", "GQ"},
        {"241", "Gabon", "GA"},
        {"242", "Republic of the Congo", "CG"},
        {"243", "DR Congo", "CD"},
        {"244", "Angola", "AO"},
        {"245", "Guinea-Bissau", "GW"},
        {"246", "Diego Garcia", "IO"},
        {"248", "Seychelles", "SC"},
        {"249", "Sudan", "SD"},
        {"250", "Rwanda", "RW"},
        {"251", "Ethiopia", "ET"},
        {"252", "Somalia", "SO"},
        {"253", "Djibouti", "DJ"},
        {"254", "Kenya", "KE"},
        {"255", "Tanzania", "TZ"},
        {"256", "Uganda", "UG"},
        {"257", "Burundi", "BI"},
        {"258", "Mozambique", "MZ"},
        {"260", "Zambia", "ZM"},
        {"261", "Madagascar", "MG"},
        {"262", "Reunion / Mayotte", "RE"},
        {"263", "Zimbabwe", "ZW"},
        {"264", "Namibia", "NA"},
        {"265", "Malawi", "MW"},
        {"266", "Lesotho", "LS"},
        {"267", "Botswana", "BW"},
        {"268", "Eswatini", "SZ"},
        {"269", "Comoros", "KM"},
        {"290", "Saint Helena", "SH"},
        {"291", "Eritrea", "ER"},
        {"297", "Aruba", "AW"},
        {"298", "Faroe Islands", "FO"},
        {"299", "Greenland", "GL"},
        {"590", "Guadeloupe", "GP"},
        {"594", "French Guiana", "GF"},
        {"596", "Martinique", "MQ"},
        {"597", "Suriname", "SR"},
        {"599", "Curacao", "CW"},
        {"670", "Timor-Leste", "TL"},
        {"672", "Australian External Territories", "AQ"},
        {"674", "Nauru", "NR"},
        {"675", "Papua New Guinea", "PG"},
        {"676", "Tonga", "TO"},
        {"677", "Solomon Islands", "SB"},
        {"678", "Vanuatu", "VU"},
        {"680", "Palau", "PW"},
        {"681", "Wallis and Futuna", "WF"},
        {"682", "Cook Islands", "CK"},
        {"683", "Niue", "NU"},
        {"685", "Samoa", "WS"},
        {"686", "Kiribati", "KI"},
        {"687", "New Caledonia", "NC"},
        {"688", "Tuvalu", "TV"},
        {"689", "French Polynesia", "PF"},
        {"690", "Tokelau", "TK"},
        {"691", "Micronesia", "FM"},
        {"692", "Marshall Islands", "MH"},
        {"853x", "", ""},
        {"886x", "", ""},
        {"994x", "", ""},
        {"995x", "", ""},
        {"996x", "", ""},
        {"998x", "", ""}
    };

    // --- Bangladesh carrier prefixes (mobile) ---
    private static final String[][] BD_CARRIERS = {
        {"013", "Grameenphone"},
        {"017", "Grameenphone"},
        {"018", "Robi"},
        {"019", "Banglalink"},
        {"016", "Airtel"},
        {"015", "Teletalk"}
    };

    // --- BD landline area codes ---
    private static final String[][] BD_AREAS = {
        {"02", "Dhaka"},
        {"031", "Chattogram"},
        {"033", "Chattogram"},
        {"035", "Cox's Bazar"},
        {"036", "Cumilla"},
        {"037", "Noakhali"},
        {"038", "Feni"},
        {"039", "Lakshmipur"},
        {"040", "Jashore"},
        {"041", "Khulna"},
        {"042", "Jessore"},
        {"043", "Barishal"},
        {"044", "Patuakhali"},
        {"045", "Bhola"},
        {"046", "Tangail"},
        {"047", "Madhabpur"},
        {"048", "Sylhet"},
        {"049", "Moulvibazar"},
        {"051", "Bogura"},
        {"052", "Sirajganj"},
        {"053", "Pabna"},
        {"054", "Bogra"},
        {"055", "Naogaon"},
        {"056", "Chapainawabganj"},
        {"058", "Dinajpur"},
        {"059", "Thakurgaon"},
        {"061", "Narayanganj"},
        {"062", "Gazipur"},
        {"063", "Faridpur"},
        {"064", "Gopalganj"},
        {"065", "Kishoreganj"},
        {"066", "Manikganj"},
        {"067", "Munshiganj"},
        {"068", "Narsingdi"},
        {"069", "Tangail"},
        {"071", "Sylhet"},
        {"072", "Rajshahi"},
        {"073", "Pabna"},
        {"074", "Dinajpur"},
        {"075", "Rangpur"},
        {"076", "Kurigram"},
        {"077", "Nilphamari"},
        {"078", "Lalmonirhat"},
        {"079", "Panchagarh"},
        {"081", "Cumilla"},
        {"082", "Chandpur"},
        {"083", "Lakshmipur"},
        {"084", "Feni"},
        {"085", "Brahmanbaria"},
        {"086", "Habiganj"},
        {"087", "Sunamganj"},
        {"088", "Netrokona"},
        {"089", "Jamalpur"},
        {"090", "Mymensingh"},
        {"091", "Mymensingh"},
        {"092", "Sherpur"},
        {"093", "Jamalpur"}
    };

    // --- US/Canada area codes ---
    private static final String[][] NANP_AREAS = {
        {"201", "New Jersey"}, {"202", "Washington, D.C."}, {"203", "Connecticut"},
        {"205", "Alabama"}, {"206", "Washington"}, {"207", "Maine"}, {"208", "Idaho"},
        {"209", "California"}, {"210", "Texas"}, {"212", "New York"}, {"213", "California"},
        {"214", "Texas"}, {"215", "Pennsylvania"}, {"216", "Ohio"}, {"217", "Illinois"},
        {"218", "Minnesota"}, {"219", "Indiana"}, {"224", "Illinois"}, {"225", "Louisiana"},
        {"227", "Maryland"}, {"228", "Mississippi"}, {"229", "Georgia"}, {"231", "Michigan"},
        {"234", "Ohio"}, {"239", "Florida"}, {"240", "Maryland"}, {"248", "Michigan"},
        {"251", "Alabama"}, {"253", "Washington"}, {"254", "Texas"}, {"256", "Alabama"},
        {"260", "Indiana"}, {"262", "Wisconsin"}, {"267", "Pennsylvania"}, {"269", "Michigan"},
        {"270", "Kentucky"}, {"272", "Pennsylvania"}, {"276", "Virginia"}, {"281", "Texas"},
        {"283", "Ohio"}, {"301", "Maryland"}, {"302", "Delaware"}, {"303", "Colorado"},
        {"304", "West Virginia"}, {"305", "Florida"}, {"307", "Wyoming"}, {"308", "Nebraska"},
        {"309", "Illinois"}, {"310", "California"}, {"312", "Illinois"}, {"313", "Michigan"},
        {"314", "Missouri"}, {"315", "New York"}, {"316", "Kansas"}, {"317", "Indiana"},
        {"318", "Louisiana"}, {"319", "Iowa"}, {"320", "Minnesota"}, {"321", "Florida"},
        {"323", "California"}, {"325", "Texas"}, {"330", "Ohio"}, {"331", "Illinois"},
        {"334", "Alabama"}, {"336", "North Carolina"}, {"337", "Louisiana"}, {"339", "Massachusetts"},
        {"346", "Texas"}, {"347", "New York"}, {"351", "Massachusetts"}, {"352", "Florida"},
        {"360", "Washington"}, {"361", "Texas"}, {"364", "Kentucky"}, {"380", "Ohio"},
        {"385", "Utah"}, {"386", "Florida"}, {"401", "Rhode Island"}, {"402", "Nebraska"},
        {"404", "Georgia"}, {"405", "Oklahoma"}, {"406", "Montana"}, {"407", "Florida"},
        {"408", "California"}, {"409", "Texas"}, {"410", "Maryland"}, {"412", "Pennsylvania"},
        {"413", "Massachusetts"}, {"414", "Wisconsin"}, {"415", "California"}, {"417", "Missouri"},
        {"419", "Ohio"}, {"423", "Tennessee"}, {"424", "California"}, {"425", "Washington"},
        {"430", "Texas"}, {"432", "Texas"}, {"434", "Virginia"}, {"435", "Utah"},
        {"440", "Ohio"}, {"442", "California"}, {"443", "Maryland"}, {"450", "Quebec"},
        {"469", "Texas"}, {"470", "Georgia"}, {"475", "Connecticut"}, {"478", "Georgia"},
        {"479", "Arkansas"}, {"480", "Arizona"}, {"484", "Pennsylvania"}, {"501", "Arkansas"},
        {"502", "Kentucky"}, {"503", "Oregon"}, {"504", "Louisiana"}, {"505", "New Mexico"},
        {"507", "Minnesota"}, {"508", "Massachusetts"}, {"509", "Washington"}, {"510", "California"},
        {"512", "Texas"}, {"513", "Ohio"}, {"515", "Iowa"}, {"516", "New York"},
        {"517", "Michigan"}, {"518", "New York"}, {"520", "Arizona"}, {"530", "California"},
        {"531", "Nebraska"}, {"534", "Wisconsin"}, {"539", "Oklahoma"}, {"540", "Virginia"},
        {"551", "New Jersey"}, {"559", "California"}, {"561", "Florida"}, {"562", "California"},
        {"563", "Iowa"}, {"567", "Ohio"}, {"570", "Pennsylvania"}, {"571", "Virginia"},
        {"573", "Missouri"}, {"574", "Indiana"}, {"575", "New Mexico"}, {"580", "Oklahoma"},
        {"585", "New York"}, {"586", "Michigan"}, {"601", "Mississippi"}, {"602", "Arizona"},
        {"603", "New Hampshire"}, {"605", "South Dakota"}, {"606", "Kentucky"}, {"607", "New York"},
        {"608", "Wisconsin"}, {"609", "New Jersey"}, {"610", "Pennsylvania"}, {"612", "Minnesota"},
        {"614", "Ohio"}, {"615", "Tennessee"}, {"616", "Michigan"}, {"617", "Massachusetts"},
        {"618", "Illinois"}, {"619", "California"}, {"620", "Kansas"}, {"623", "Arizona"},
        {"626", "California"}, {"628", "California"}, {"630", "Illinois"}, {"631", "New York"},
        {"636", "Missouri"}, {"641", "Iowa"}, {"646", "New York"}, {"650", "California"},
        {"651", "Minnesota"}, {"660", "Missouri"}, {"661", "California"}, {"662", "Mississippi"},
        {"667", "Maryland"}, {"669", "California"}, {"678", "Georgia"}, {"680", "New York"},
        {"681", "West Virginia"}, {"682", "Texas"}, {"701", "North Dakota"}, {"702", "Nevada"},
        {"703", "Virginia"}, {"704", "North Carolina"}, {"706", "Georgia"}, {"707", "California"},
        {"708", "Illinois"}, {"712", "Iowa"}, {"713", "Texas"}, {"714", "California"},
        {"715", "Wisconsin"}, {"716", "New York"}, {"717", "Pennsylvania"}, {"718", "New York"},
        {"719", "Colorado"}, {"720", "Colorado"}, {"724", "Pennsylvania"}, {"725", "Nevada"},
        {"727", "Florida"}, {"731", "Tennessee"}, {"732", "New Jersey"}, {"734", "Michigan"},
        {"737", "Texas"}, {"740", "Ohio"}, {"747", "California"}, {"754", "Florida"},
        {"757", "Virginia"}, {"760", "California"}, {"762", "Georgia"}, {"763", "Minnesota"},
        {"765", "Indiana"}, {"769", "Mississippi"}, {"770", "Georgia"}, {"772", "Florida"},
        {"773", "Illinois"}, {"774", "Massachusetts"}, {"775", "Nevada"}, {"779", "Illinois"},
        {"801", "Utah"}, {"802", "Vermont"}, {"803", "South Carolina"}, {"804", "Virginia"},
        {"805", "California"}, {"806", "Texas"}, {"808", "Hawaii"}, {"810", "Michigan"},
        {"812", "Indiana"}, {"813", "Florida"}, {"814", "Pennsylvania"}, {"815", "Illinois"},
        {"816", "Missouri"}, {"817", "Texas"}, {"818", "California"}, {"828", "North Carolina"},
        {"830", "Texas"}, {"831", "California"}, {"832", "Texas"}, {"843", "South Carolina"},
        {"845", "New York"}, {"847", "Illinois"}, {"848", "New Jersey"}, {"850", "Florida"},
        {"856", "New Jersey"}, {"857", "Massachusetts"}, {"858", "California"}, {"859", "Kentucky"},
        {"860", "Connecticut"}, {"862", "New Jersey"}, {"863", "Florida"}, {"864", "South Carolina"},
        {"865", "Tennessee"}, {"870", "Arkansas"}, {"872", "Illinois"}, {"878", "Pennsylvania"},
        {"901", "Tennessee"}, {"903", "Texas"}, {"904", "Florida"}, {"906", "Michigan"},
        {"907", "Alaska"}, {"908", "New Jersey"}, {"909", "California"}, {"910", "North Carolina"},
        {"912", "Georgia"}, {"913", "Kansas"}, {"914", "New York"}, {"915", "Texas"},
        {"916", "California"}, {"917", "New York"}, {"918", "Oklahoma"}, {"919", "North Carolina"},
        {"920", "Wisconsin"}, {"925", "California"}, {"928", "Arizona"}, {"929", "New York"},
        {"930", "Indiana"}, {"931", "Tennessee"}, {"936", "Texas"}, {"937", "Ohio"},
        {"940", "Texas"}, {"941", "Florida"}, {"947", "Michigan"}, {"949", "California"},
        {"951", "California"}, {"952", "Minnesota"}, {"954", "Florida"}, {"956", "Texas"},
        {"959", "Connecticut"}, {"970", "Colorado"}, {"971", "Oregon"}, {"972", "Texas"},
        {"973", "New Jersey"}, {"978", "Massachusetts"}, {"979", "Texas"}, {"980", "North Carolina"},
        {"984", "North Carolina"}, {"985", "Louisiana"}, {"986", "Idaho"}, {"989", "Michigan"}
    };

    // ------------------------------------------------------------------

    /** Keeps +, digits; strips dashes, spaces, brackets, dots. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '+') {
                if (sb.length() == 0) {
                    sb.append(ch);
                }
            } else if (ch >= '0' && ch <= '9') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /** Digits only (drops leading +). */
    public static String digits(String raw) {
        String n = normalize(raw);
        if (n.startsWith("+")) {
            return n.substring(1);
        }
        return n;
    }

    /**
     * Stable grouping key so "01712-345678", "8801712345678" and
     * "+880 1712-345678" land in the same bucket.
     */
    public static String groupKey(String raw) {
        String d = digits(raw);
        if (d.startsWith("880") && d.length() >= 13) {
            return "0" + d.substring(3);
        }
        if (d.length() == 11 && d.startsWith("01")) {
            return d;
        }
        if (d.length() == 10 && (d.startsWith("1") || d.charAt(0) >= '2')) {
            // NANP without leading 1 stays as-is; also plain 10-digit local
            return d;
        }
        if (d.startsWith("00")) {
            String rest = d.substring(2);
            if (rest.startsWith("880") && rest.length() >= 13) {
                return "0" + rest.substring(3);
            }
            return rest;
        }
        return d;
    }

    /** Full analysis. */
    public static Info analyze(String raw) {
        Info info = new Info();
        info.raw = raw == null ? "" : raw.trim();
        String n = normalize(raw);
        info.pretty = pretty(n);

        if (n.length() == 0) {
            return info;
        }

        String national = n;
        boolean intl = false;

        if (n.startsWith("+")) {
            intl = true;
            national = n.substring(1);
        } else if (n.startsWith("00") && n.length() > 4) {
            intl = true;
            national = n.substring(2);
        }

        if (intl) {
            matchCountry(info, national);
            national = stripCountry(info, national);
            if (national.length() == 0) {
                national = info.dialCode;
            }
            fillInternationalType(info, national);
            info.region = "";
            info.carrier = "";
            // NANP area lookup
            if ("US".equals(info.iso) && national.length() >= 10) {
                String rest = national.length() > 10 && national.charAt(0) == '1'
                        ? national.substring(1) : national;
                if (rest.length() >= 10) {
                    String area = rest.substring(0, 3);
                    String state = lookup(NANP_AREAS, area);
                    if (state != null) {
                        info.region = state;
                    }
                }
            }
            if ("IN".equals(info.iso)) {
                String area = areaOfIndianLandline(national);
                String city = area == null ? null : lookup(BD_AREAS, area);
                // reuse Indian city table below
                city = lookup(IN_AREAS, area);
                if (city != null) {
                    info.region = city;
                }
                if (national.length() == 10 && national.charAt(0) >= '6') {
                    info.region = "India (mobile)";
                }
            }
            if ("GB".equals(info.iso)) {
                info.region = regionUk(national);
            }
            if (info.region.length() == 0) {
                info.region = countryRegion(info);
            }
            return info;
        }

        // ---- local numbers ----
        String digitsOnly = n;

        // Bangladesh mobile: 01[3-9]XXXXXXXX (11 digits)
        if (digitsOnly.length() == 11 && digitsOnly.startsWith("01")
                && digitsOnly.charAt(2) >= '3' && digitsOnly.charAt(2) <= '9') {
            info.country = "Bangladesh";
            info.iso = "BD";
            info.dialCode = "+880";
            info.type = TYPE_MOBILE;
            info.carrier = carrierBd(digitsOnly);
            info.region = "Bangladesh (nationwide)";
            return info;
        }

        // Bangladesh landline: 0[2-9]XXXXXXX (10 digits)
        if (digitsOnly.length() == 10 && digitsOnly.charAt(0) == '0'
                && digitsOnly.charAt(1) >= '2' && digitsOnly.charAt(1) <= '9') {
            info.country = "Bangladesh";
            info.iso = "BD";
            info.dialCode = "+880";
            info.type = TYPE_LANDLINE;
            String area = digitsOnly.substring(0, 2);
            String region = lookup(BD_AREAS, area);
            if (region == null && digitsOnly.length() >= 3) {
                region = lookup(BD_AREAS, digitsOnly.substring(0, 3));
            }
            info.region = region == null ? "Bangladesh" : region + ", Bangladesh";
            info.carrier = "BDTL (landline)";
            return info;
        }

        // India mobile: [6-9]XXXXXXXXXX (10 digits)
        if (digitsOnly.length() == 10 && digitsOnly.charAt(0) >= '6' && digitsOnly.charAt(0) <= '9') {
            info.country = "India";
            info.iso = "IN";
            info.dialCode = "+91";
            info.type = TYPE_MOBILE;
            info.region = "India (mobile)";
            return info;
        }

        // NANP: 1XXXXXXXXXX or XXXXXXXXXX
        String nanp = null;
        if (digitsOnly.length() == 11 && digitsOnly.charAt(0) == '1'
                && digitsOnly.charAt(1) >= '2' && digitsOnly.charAt(1) <= '9') {
            nanp = digitsOnly.substring(1);
        } else if (digitsOnly.length() == 10 && digitsOnly.charAt(0) >= '2' && digitsOnly.charAt(0) <= '9') {
            // Could be NANP or an Indian landline; prefer NANP for 10-digit 2-9 starts
            // only when it matches a known area pattern later; treat as local unknown.
            nanp = digitsOnly;
        }
        if (nanp != null && isNanpLocal(nanp)) {
            info.country = "United States / Canada";
            info.iso = "US";
            info.dialCode = "+1";
            info.type = TYPE_LANDLINE;
            String area = nanp.substring(0, 3);
            String state = lookup(NANP_AREAS, area);
            info.region = state == null ? "North America" : state;
            info.carrier = "";
            return info;
        }

        // UK local: 0XXXXXXXXXX (11 digits starting 0)
        if (digitsOnly.length() == 11 && digitsOnly.charAt(0) == '0') {
            String area = digitsOnly.substring(0, 3);
            if (lookup(GB_AREAS, area) != null || digitsOnly.startsWith("07")) {
                info.country = "United Kingdom";
                info.iso = "GB";
                info.dialCode = "+44";
                info.type = digitsOnly.startsWith("07") ? TYPE_MOBILE : TYPE_LANDLINE;
                info.region = regionUk(digitsOnly);
                return info;
            }
        }

        // Short codes (service numbers): 1-6 digits
        if (digitsOnly.length() <= 6) {
            info.type = TYPE_SHORT;
            info.region = "";
            return info;
        }

        // Unknown local
        info.type = TYPE_UNKNOWN;
        return info;
    }

    private static boolean isNanpLocal(String nanp) {
        if (nanp.length() != 10) {
            return false;
        }
        char c0 = nanp.charAt(0);
        char c1 = nanp.charAt(1);
        if (c0 < '2' || c0 > '9' || c1 < '2' || c1 > '9') {
            return false;
        }
        return true;
    }

    private static final String[][] IN_AREAS = {
        {"011", "Delhi"}, {"020", "Pune"}, {"022", "Mumbai"}, {"033", "Kolkata"},
        {"040", "Hyderabad"}, {"044", "Chennai"}, {"079", "Ahmedabad"}, {"080", "Bengaluru"},
        {"0471", "Thiruvananthapuram"}, {"044", "Chennai"}
    };

    private static final String[][] GB_AREAS = {
        {"020", "London"}, {"023", "Southampton"}, {"024", "Coventry"}, {"028", "Belfast"},
        {"029", "Cardiff"}, {"0113", "Leeds"}, {"0114", "Sheffield"}, {"0115", "Nottingham"},
        {"0116", "Leicester"}, {"0117", "Bristol"}, {"0118", "Reading"}, {"0121", "Birmingham"},
        {"0131", "Edinburgh"}, {"0141", "Glasgow"}, {"0151", "Liverpool"}, {"0161", "Manchester"},
        {"0191", "Newcastle"}, {"0142", "Bath"}, {"0143", "Warrington"}, {"0147", "Gloucester"},
        {"0148", "Hereford"}, {"0149", "Norwich"}, {"0152", "Plymouth"}, {"0153", "King's Lynn"},
        {"0155", "Hull"}, {"0158", "Worcester"}, {"0160", "Chester"}, {"0162", "Ipswich"},
        {"0163", "Colchester"}, {"0164", "Barnstaple"}, {"0165", "Ilfracombe"},
        {"0166", "Cambridge"}, {"0167", "Wisbech"}, {"0168", "Ely"}, {"0169", "Louth"},
        {"0172", "Truro"}, {"0175", "Grimsby"}, {"0176", "Wakefield"}, {"0177", "Welshpool"},
        {"0179", "Rugby"}, {"0180", "Banbury"}, {"0182", "Bicester"}, {"0183", "Nuneaton"},
        {"0184", "Harrogate"}, {"0185", "Altrincham"}, {"0186", "Macclesfield"},
        {"0187", "Redditch"}, {"0188", "Barnard Castle"}, {"0189", "Oxford"},
        {"0190", "Durham"}, {"0191", "Newcastle"}, {"0192", "Southport"}, {"0193", "Bude"},
        {"0194", "Gloucester"}, {"0195", "Worksop"}, {"0196", "Haverfordwest"},
        {"0197", "Basingstoke"}, {"0198", "Newton Abbot"}, {"0199", "Launceston"}
    };

    private static String regionUk(String national) {
        if (national.startsWith("07")) {
            return "United Kingdom (mobile)";
        }
        String area = national.length() >= 4 ? national.substring(0, 4) : national.substring(0, 3);
        String region = lookup(GB_AREAS, area);
        if (region == null) {
            region = lookup(GB_AREAS, national.substring(0, 3));
        }
        return region == null ? "United Kingdom" : region + ", UK";
    }

    private static String areaOfIndianLandline(String national) {
        if (national.length() >= 4 && national.charAt(0) == '0') {
            return national.substring(0, 4);
        }
        return null;
    }

    private static void matchCountry(Info info, String national) {
        String bestCode = null;
        String[] best = null;
        for (int i = 0; i < COUNTRIES.length; i++) {
            String code = COUNTRIES[i][0];
            if (code.length() > 3 || code.indexOf('x') >= 0) {
                continue;
            }
            if (national.startsWith(code)) {
                if (bestCode == null || code.length() > bestCode.length()) {
                    bestCode = code;
                    best = COUNTRIES[i];
                }
            }
        }
        if (best != null) {
            info.dialCode = "+" + bestCode;
            info.country = best[1];
            info.iso = best[2];
        } else {
            info.country = "Unknown";
            info.iso = "";
            info.dialCode = "";
        }
    }

    private static String stripCountry(Info info, String national) {
        if (info.dialCode.length() > 1) {
            String code = info.dialCode.substring(1);
            if (national.startsWith(code)) {
                return national.substring(code.length());
            }
        }
        return national;
    }

    private static void fillInternationalType(Info info, String national) {
        String d = national;
        if (d.length() == 0) {
            info.type = TYPE_UNKNOWN;
            return;
        }
        // toll-free / premium shared codes
        if (d.startsWith("800") || d.startsWith("808") || d.startsWith("877")
                || d.startsWith("866") || d.startsWith("855") || d.startsWith("844")
                || d.startsWith("833") || d.startsWith("888") || d.startsWith("878")
                || d.startsWith("880") || d.startsWith("881") || d.startsWith("882")) {
            info.type = TYPE_TOLLFREE;
            return;
        }
        if (d.startsWith("900") || d.startsWith("976") || d.startsWith("979")
                || d.startsWith("901") || d.startsWith("902") || d.startsWith("903")
                || d.startsWith("904") || d.startsWith("905") || d.startsWith("906")
                || d.startsWith("907") || d.startsWith("908") || d.startsWith("909")) {
            info.type = TYPE_PREMIUM;
            return;
        }
        if (d.length() <= 6) {
            info.type = TYPE_SHORT;
            return;
        }
        if (info.isLandline()) {
            return;
        }
        // decide mobile vs landline by country heuristics
        if ("BD".equals(info.iso)) {
            info.type = d.startsWith("1") ? TYPE_MOBILE : TYPE_LANDLINE;
        } else if ("IN".equals(info.iso)) {
            info.type = d.length() == 10 && d.charAt(0) >= '6' ? TYPE_MOBILE : TYPE_LANDLINE;
        } else if ("US".equals(info.iso)) {
            info.type = TYPE_LANDLINE;
        } else {
            info.type = TYPE_INTERNATIONAL;
        }
    }

    private static String carrierBd(String digits) {
        String prefix = digits.substring(0, 3);
        String name = lookup(BD_CARRIERS, prefix);
        if (name != null) {
            return name;
        }
        return "";
    }

    private static String lookup(String[][] table, String key) {
        if (key == null) {
            return null;
        }
        for (int i = 0; i < table.length; i++) {
            if (table[i][0].equals(key)) {
                return table[i][1];
            }
        }
        return null;
    }

    private static String countryRegion(Info info) {
        if (info.country.length() == 0) {
            return "";
        }
        return info.country;
    }

    /** Human-friendly grouping of digits. */
    public static String pretty(String n) {
        if (n == null || n.length() == 0) {
            return "";
        }
        String digitsOnly = n.startsWith("+") ? n.substring(1) : n;
        boolean plus = n.startsWith("+");

        // Bangladesh mobile: 01712345678 -> 01712 345678
        if (!plus && digitsOnly.length() == 11 && digitsOnly.startsWith("01")) {
            return digitsOnly.substring(0, 5) + " " + digitsOnly.substring(5);
        }
        if (plus && digitsOnly.startsWith("8801") && digitsOnly.length() == 13) {
            String local = "0" + digitsOnly.substring(3);
            return "+880 " + local.substring(0, 5) + " " + local.substring(5);
        }
        // NANP: (415) 555-0123
        if (digitsOnly.length() == 10 && !plus && digitsOnly.charAt(0) >= '2') {
            return "(" + digitsOnly.substring(0, 3) + ") " + digitsOnly.substring(3, 6)
                    + "-" + digitsOnly.substring(6);
        }
        if (digitsOnly.length() == 11 && digitsOnly.charAt(0) == '1' && !plus) {
            return "(" + digitsOnly.substring(1, 4) + ") " + digitsOnly.substring(4, 7)
                    + "-" + digitsOnly.substring(7);
        }
        if (plus) {
            if (digitsOnly.length() <= 3) {
                return "+" + digitsOnly;
            }
            if (digitsOnly.length() <= 7) {
                return "+" + digitsOnly.substring(0, 2) + " " + digitsOnly.substring(2);
            }
            return "+" + digitsOnly.substring(0, 2) + " " + digitsOnly.substring(2, 6)
                    + " " + digitsOnly.substring(6);
        }
        // generic: groups of 3 from the right
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = digitsOnly.length();
        int firstGroup = len % 3;
        if (firstGroup == 0) {
            firstGroup = 3;
        }
        sb.append(digitsOnly.substring(0, firstGroup));
        i = firstGroup;
        while (i < len) {
            sb.append(' ').append(digitsOnly.substring(i, Math.min(i + 3, len)));
            i += 3;
        }
        return sb.toString();
    }

    /** Convenience: short label for list rows, e.g. "Dhaka" or "United States / Canada". */
    public static String shortLabel(Info info) {
        if (info.region.length() > 0 && info.region.indexOf(',') > 0) {
            return info.region.substring(0, info.region.indexOf(','));
        }
        if (info.region.length() > 0 && info.region.indexOf('(') > 0) {
            return info.region.substring(0, info.region.indexOf('(')).trim();
        }
        if (info.region.length() > 0) {
            return info.region;
        }
        if (info.country.length() > 0) {
            return info.country;
        }
        return "";
    }

    /** Type label in English (kept here so utilities stay context-free). */
    public static String typeLabel(String type) {
        if (TYPE_MOBILE.equals(type)) {
            return "Mobile";
        }
        if (TYPE_LANDLINE.equals(type)) {
            return "Landline";
        }
        if (TYPE_TOLLFREE.equals(type)) {
            return "Toll-free";
        }
        if (TYPE_PREMIUM.equals(type)) {
            return "Premium rate";
        }
        if (TYPE_SHORT.equals(type)) {
            return "Short code";
        }
        if (TYPE_INTERNATIONAL.equals(type)) {
            return "International";
        }
        return "Unknown";
    }

    /** Digits→T9 key for name search. */
    public static String t9(String text) {
        if (text == null) {
            return "";
        }
        String upper = text.toUpperCase(Locale.US);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                sb.append(t9Digit(c));
            } else if (c == ' ') {
                // skip
            }
        }
        return sb.toString();
    }

    private static char t9Digit(char c) {
        if (c >= 'A' && c <= 'C') return '2';
        if (c >= 'D' && c <= 'F') return '3';
        if (c >= 'G' && c <= 'I') return '4';
        if (c >= 'J' && c <= 'L') return '5';
        if (c >= 'M' && c <= 'O') return '6';
        if (c >= 'P' && c <= 'S') return '7';
        if (c >= 'T' && c <= 'V') return '8';
        return '9';
    }
}
