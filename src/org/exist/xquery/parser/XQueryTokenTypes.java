// $ANTLR 2.7.7 (2006-11-01): "XQuery.g" -> "XQueryParser.java"$

	package org.exist.xquery.parser;

	import antlr.debug.misc.*;
	import java.io.StringReader;
	import java.io.BufferedReader;
	import java.io.InputStreamReader;
	import java.util.ArrayDeque;
	import java.util.ArrayList;
	import java.util.Deque;
	import java.util.List;
	import java.util.Iterator;
	import org.exist.storage.BrokerPool;
	import org.exist.storage.DBBroker;
	import org.exist.EXistException;
	import org.exist.dom.persistent.DocumentSet;
	import org.exist.dom.persistent.DocumentImpl;
	import org.exist.dom.QName;
	import org.exist.security.PermissionDeniedException;
	import org.exist.xquery.*;
	import org.exist.xquery.value.*;
	import org.exist.xquery.functions.fn.*;

public interface XQueryTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int QNAME = 4;
	int EQNAME = 5;
	int PREDICATE = 6;
	int FLWOR = 7;
	int PARENTHESIZED = 8;
	int ABSOLUTE_SLASH = 9;
	int ABSOLUTE_DSLASH = 10;
	int WILDCARD = 11;
	int PREFIX_WILDCARD = 12;
	int FUNCTION = 13;
	int DYNAMIC_FCALL = 14;
	int UNARY_MINUS = 15;
	int UNARY_PLUS = 16;
	int XPOINTER = 17;
	int XPOINTER_ID = 18;
	int VARIABLE_REF = 19;
	int VARIABLE_BINDING = 20;
	int ELEMENT = 21;
	int ATTRIBUTE = 22;
	int ATTRIBUTE_CONTENT = 23;
	int TEXT = 24;
	int VERSION_DECL = 25;
	int NAMESPACE_DECL = 26;
	int DEF_NAMESPACE_DECL = 27;
	int DEF_COLLATION_DECL = 28;
	int DEF_FUNCTION_NS_DECL = 29;
	int CONTEXT_ITEM_DECL = 30;
	int ANNOT_DECL = 31;
	int GLOBAL_VAR = 32;
	int FUNCTION_DECL = 33;
	int INLINE_FUNCTION_DECL = 34;
	int FUNCTION_INLINE = 35;
	int FUNCTION_TEST = 36;
	int MAP = 37;
	int MAP_TEST = 38;
	int LOOKUP = 39;
	int ARRAY = 40;
	int ARRAY_TEST = 41;
	int PROLOG = 42;
	int OPTION = 43;
	int ATOMIC_TYPE = 44;
	int MODULE = 45;
	int ORDER_BY = 46;
	int GROUP_BY = 47;
	int POSITIONAL_VAR = 48;
	int CATCH_ERROR_CODE = 49;
	int CATCH_ERROR_DESC = 50;
	int CATCH_ERROR_VAL = 51;
	int MODULE_DECL = 52;
	int MODULE_IMPORT = 53;
	int SCHEMA_IMPORT = 54;
	int ATTRIBUTE_TEST = 55;
	int COMP_ELEM_CONSTRUCTOR = 56;
	int COMP_ATTR_CONSTRUCTOR = 57;
	int COMP_TEXT_CONSTRUCTOR = 58;
	int COMP_COMMENT_CONSTRUCTOR = 59;
	int COMP_PI_CONSTRUCTOR = 60;
	int COMP_NS_CONSTRUCTOR = 61;
	int COMP_DOC_CONSTRUCTOR = 62;
	int PRAGMA = 63;
	int GTEQ = 64;
	int SEQUENCE = 65;
	int LITERAL_xpointer = 66;
	int LPAREN = 67;
	int RPAREN = 68;
	int NCNAME = 69;
	int LITERAL_xquery = 70;
	int LITERAL_version = 71;
	int SEMICOLON = 72;
	int LITERAL_module = 73;
	int LITERAL_namespace = 74;
	int EQ = 75;
	int STRING_LITERAL = 76;
	int LITERAL_declare = 77;
	int LITERAL_default = 78;
	// "boundary-space" = 79
	int LITERAL_ordering = 80;
	int LITERAL_construction = 81;
	// "base-uri" = 82
	// "copy-namespaces" = 83
	int LITERAL_option = 84;
	int LITERAL_function = 85;
	int LITERAL_variable = 86;
	int LITERAL_context = 87;
	int LITERAL_item = 88;
	int MOD = 89;
	int LITERAL_import = 90;
	int LITERAL_encoding = 91;
	int LITERAL_collation = 92;
	int LITERAL_element = 93;
	int LITERAL_order = 94;
	int LITERAL_empty = 95;
	int LITERAL_greatest = 96;
	int LITERAL_least = 97;
	int LITERAL_preserve = 98;
	int LITERAL_strip = 99;
	int LITERAL_ordered = 100;
	int LITERAL_unordered = 101;
	int COMMA = 102;
	// "no-preserve" = 103
	int LITERAL_inherit = 104;
	// "no-inherit" = 105
	int DOLLAR = 106;
	int LITERAL_external = 107;
	int COLON = 108;
	int LCURLY = 109;
	int RCURLY = 110;
	int LITERAL_schema = 111;
	int BRACED_URI_LITERAL = 112;
	int LITERAL_as = 113;
	int LITERAL_at = 114;
	// "empty-sequence" = 115
	int QUESTION = 116;
	int STAR = 117;
	int PLUS = 118;
	int LITERAL_map = 119;
	int LITERAL_array = 120;
	int LITERAL_for = 121;
	int LITERAL_let = 122;
	int LITERAL_try = 123;
	int LITERAL_some = 124;
	int LITERAL_every = 125;
	int LITERAL_if = 126;
	int LITERAL_switch = 127;
	int LITERAL_typeswitch = 128;
	int LITERAL_update = 129;
	int LITERAL_replace = 130;
	int LITERAL_value = 131;
	int LITERAL_insert = 132;
	int LITERAL_delete = 133;
	int LITERAL_rename = 134;
	int LITERAL_with = 135;
	int LITERAL_into = 136;
	int LITERAL_preceding = 137;
	int LITERAL_following = 138;
	int LITERAL_catch = 139;
	int UNION = 140;
	int LITERAL_return = 141;
	int LITERAL_where = 142;
	int LITERAL_in = 143;
	int LITERAL_allowing = 144;
	int LITERAL_by = 145;
	int LITERAL_stable = 146;
	int LITERAL_ascending = 147;
	int LITERAL_descending = 148;
	int LITERAL_group = 149;
	int LITERAL_satisfies = 150;
	int LITERAL_case = 151;
	int LITERAL_then = 152;
	int LITERAL_else = 153;
	int LITERAL_or = 154;
	int LITERAL_and = 155;
	int LITERAL_instance = 156;
	int LITERAL_of = 157;
	int LITERAL_treat = 158;
	int LITERAL_castable = 159;
	int LITERAL_cast = 160;
	int BEFORE = 161;
	int AFTER = 162;
	int LITERAL_eq = 163;
	int LITERAL_ne = 164;
	int LITERAL_lt = 165;
	int LITERAL_le = 166;
	int LITERAL_gt = 167;
	int LITERAL_ge = 168;
	int GT = 169;
	int NEQ = 170;
	int LT = 171;
	int LTEQ = 172;
	int LITERAL_is = 173;
	int LITERAL_isnot = 174;
	int CONCAT = 175;
	int LITERAL_to = 176;
	int MINUS = 177;
	int LITERAL_div = 178;
	int LITERAL_idiv = 179;
	int LITERAL_mod = 180;
	int BANG = 181;
	int PRAGMA_START = 182;
	int PRAGMA_END = 183;
	int LITERAL_union = 184;
	int LITERAL_intersect = 185;
	int LITERAL_except = 186;
	int SLASH = 187;
	int DSLASH = 188;
	int LITERAL_text = 189;
	int LITERAL_node = 190;
	int LITERAL_attribute = 191;
	int LITERAL_comment = 192;
	// "namespace-node" = 193
	// "processing-instruction" = 194
	// "document-node" = 195
	int LITERAL_document = 196;
	int HASH = 197;
	int SELF = 198;
	int XML_COMMENT = 199;
	int XML_PI = 200;
	int LPPAREN = 201;
	int STRING_CONSTRUCTOR_START = 202;
	int RPPAREN = 203;
	int AT = 204;
	int PARENT = 205;
	int LITERAL_child = 206;
	int LITERAL_self = 207;
	int LITERAL_descendant = 208;
	// "descendant-or-self" = 209
	// "following-sibling" = 210
	int LITERAL_parent = 211;
	int LITERAL_ancestor = 212;
	// "ancestor-or-self" = 213
	// "preceding-sibling" = 214
	int ARROW_OP = 215;
	int INTEGER_LITERAL = 216;
	int STRING_CONSTRUCTOR_END = 217;
	int STRING_CONSTRUCTOR_CONTENT = 218;
	int STRING_CONSTRUCTOR_INTERPOLATION_START = 219;
	int STRING_CONSTRUCTOR_INTERPOLATION_END = 220;
	int DOUBLE_LITERAL = 221;
	int DECIMAL_LITERAL = 222;
	// "schema-element" = 223
	int END_TAG_START = 224;
	int QUOT = 225;
	int APOS = 226;
	int QUOT_ATTRIBUTE_CONTENT = 227;
	int ESCAPE_QUOT = 228;
	int APOS_ATTRIBUTE_CONTENT = 229;
	int ESCAPE_APOS = 230;
	int ELEMENT_CONTENT = 231;
	int XML_COMMENT_END = 232;
	int XML_PI_END = 233;
	int XML_CDATA = 234;
	int LITERAL_collection = 235;
	int LITERAL_validate = 236;
	int XML_PI_START = 237;
	int XML_CDATA_START = 238;
	int XML_CDATA_END = 239;
	int LETTER = 240;
	int DIGITS = 241;
	int HEX_DIGITS = 242;
	int WS = 243;
	int XQDOC_COMMENT = 244;
	int EXPR_COMMENT = 245;
	int PREDEFINED_ENTITY_REF = 246;
	int CHAR_REF = 247;
	int S = 248;
	int NEXT_TOKEN = 249;
	int NAME_START_CHAR = 250;
	int NAME_CHAR = 251;
	int CHAR = 252;
	int BASECHAR = 253;
	int IDEOGRAPHIC = 254;
	int COMBINING_CHAR = 255;
	int DIGIT = 256;
	int EXTENDER = 257;
}
