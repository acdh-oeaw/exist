xquery version "1.0";

declare namespace request="http://exist-db.org/xquery/request";
declare namespace xmldb="http://exist-db.org/xquery/xmldb";

let $name := request:request-parameter("name", "")

(: The first time store the document holding he names :)
let $collection := xmldb:collection("xmldb:exist:///db", "guest", "guest")
let $dummy := 
if ( not ( doc( "names.xml" )) ) then
  xmldb:store($collection, "names.xml", <names/> )
else <dummy/>
(: Create an XUpdate document :)
let $xupdate :=
<xupdate:modifications version="1.0"
  xmlns:xupdate="http://www.xmldb.org/xupdate" >
  <xupdate:append select=" doc( 'names.xml' )/names">
    <xupdate:element name="name">{$name}</xupdate:element>
  </xupdate:append>
</xupdate:modifications>

let $dummy2 := 
if ( not ( doc( "names.xml" )/names/name = $name) ) then
xmldb:update($collection, $xupdate)
else <dummy/>
let $names-from-db := doc( "/db/names.xml" )
let $names := $names-from-db
return 
<html>
  <form method="GET">
    Please enter your name: 
    <input type="text" size="40" name="name" />
    <input type="submit" />
  </form>
  
  Hello { $names } !

</html>
