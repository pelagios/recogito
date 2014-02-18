# Recogito

A Web-based tool for validating &amp; correcting geo-resolution results.

## Installation

* Install the [Play Framework v2.2.1](http://www.playframework.com/download).
* Recogito depends on the _scalagios-core_ and _scalagios-gazetteer_ utility libraries from the [Scalagios](http://github.com/pelagios/scalagios)
  project. These are not yet available through a Maven repository. You need to add them manually to a `lib` folder in the project root.
* Start Recogito using `play start` or `play "start {portnumber}"`.  

## Importing Documents

To work with Recogito, you first need to import data. The _admin_ area includes a facility to upload a ZIP file with document metadata and 
accompanying UTF-8 plaintext files.

Document metadata must be provided as a JSON file. The name of the file can be chosen arbitrarily. The only requirement is that it has a
`.json` extension. The JSON structure defines the document's __title__, __description__ and __source__ properties, as well as the __parts__
the document consists of, and where in the ZIP file the text for the document (or its parts) are located.

A possible ZIP folder structure is e.g.:

```
isidore.json
texts/Isidore_Book IX.txt
texts/Isidore_Book XIII.txt
texts/Isidore_Book XIV.txt
``` 
The contents of the file `isidore.json` should look like this:

```javascript
{
  "title": "Isidore of Seville, Etymyologiae (English)",
  "parts": [{
    "title": "Book IX",
    "text": "texts/Isidore_Book IX.txt"
  },{
    "title": "Book XIII",
    "text": "texts/Isidore_Book XIII.txt"
  },{
    "title": "Book XIV",
    "text": "texts/Isidore_Book XIV.txt"
  }] 
}
```

The ZIP file can also contain data for multiple documents. In this case, each document must be defined in its own JSON file.

## Importing Annotations

You can import annotations as CSV files. E.g. if you want to upload automatically generated annotations before you start 
manual annotation, or to restore the results of previous work. In general the order of the columns is irrelevant, it is only
necessary to use the correct pre-defined column labels.

An example CSV file is shown below:

```csv
gdoc_part;status;toponym;offset;gazetteer_uri;
Book IX;NOT_VERIFIED;Greek;1647;http://pleiades.stoa.org/places/59649;
Book IX;NOT_VERIFIED;Athens;1795;http://pleiades.stoa.org/places/579885;
Book IX;NOT_VERIFIED;Greece;1842;;
Book IX;NOT_VERIFIED;Italy;2287;http://pleiades.stoa.org/places/456048;
Book IX;NOT_VERIFIED;Salii;2371;http://pleiades.stoa.org/places/99034;
```

If the annotations pertain to a document that has parts, the ``gdoc_part`` column must match the name of the part. The other
columns match with the fields in the Recogito data model, and are (generally) optional.

## Hacking on Recogito

* To start Recogito in development mode, type `play run`
* To create an Eclipse project, type `play eclipse` 
* To create an Eclipse project with dependency's sources attached, type `play` to enter the Play console, and then `eclipse with-source=true`

## License

Recogito is licensed under the [GNU General Public License v3.0](http://www.gnu.org/licenses/gpl.html).
