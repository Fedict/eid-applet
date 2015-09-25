# Get The Facts on Signatures #

This document will guide you through the various options you have for integrating electronic signatures within your business processes. We first give an executive summary via the following table.

| **Aspect** | **PDF signatures** | **XML signatures** |
|:-----------|:-------------------|:-------------------|
| Open Standard | yes                | yes                |
| Data Format | PDF                | whatever you want it to be |
| Easy of implementation | limited choice: iText or via proprietary products | lightweight via various open source solutions |
| Business Integration | almost forced using proprietary products to extract data | you can define your own XML schema according to your business needs |
| Validation | proprietary product specific as people will want valid signatures in Acrobat Reader | under your own control |
| Service Directive readiness | PAdES is premature | XAdES is a proven standard that has been implemented in various open source projects |
| Long-Term Accessibility | your PDFs just might survive the Adobe products | no matter how the IT landscape changes, there will always be some notepad |
| Long-Term Signature Validity | PAdES is premature, so is PAdES-A | XAdES-A has proven implementations (ETSI XAdES plugtests) |
| Risk for Vendor lock-in | high. What if Adobe decides to charge for the Reader? | low. XML parsers are all over the place |
| Preservation of Visualization | PDF/A is cool. But what if the proprietary product is buggy? | via XSLT to HTML will always work |
| Visualization of Signatures | PDF has this feature. Is human interpretation this important? | the business application should handle this aspect |


## Open Standard ##

'Open' should not only mean publishing your specifications online somewhere and have a standardization body ratify this.
'Open' should also include the ability for developers to implement the standard on multiple platforms.

And this is where PDF signatures fail. It is very hard for one to implement PDF signatures using open source libraries.
Especially since iText changed its license model to AGPL, limiting the  usage of the library (a commercial version is available however)

On the other hand, I (Frank) was able to implement XAdES-X-L version 1.4.2 in less than a week as part of our open source eID Digital Signature Service product.


## Data Format ##

When you choose for PAdES signatures, you are forced to use PDF as data container. Of course PDF can embed other data formats, but accessing this data often requires proprietary products and/or additional tooling.

XAdES is of course very well suited for XML-based formats, but can also handle binary files including (but not limited to) PDF.
Commodity office document formats  like ODF and OOXML (Microsoft Office, OpenOffice.org etc) offer native support for XML signatures, and thus allow for XAdES although not all products implement this feature.


## Ease of implementation ##

## Business Integration ##

Allowing a business to define its own XML schema according to the workflow that needs to be implemented, gives maximum freedom of choice.

Of course, PDF also allows for embedding XML fragments, but adding/extracting this XML data often requires using additional tooling: most PDF-products aren't designed for handling XML, and XML tools are not used to deal with PDF.

XML on the other hand is the most versatile structure to handle business data.

## Validation ##

This is a very interesting topic. Two approaches are possible here. On is a document-centric approach, the other is a service-centric approach.

In the document-centric approach we expect users to upload/sign/download documents and store them somewhere. As one can imagine such an approach does not integrate very well in a web based workflow.
Even if people exchange signed documents by mail, we still have to fight the difference in validation implementation by the different desktop tools.

In a service-centric approach, we sort of move the document to the background, i.e. the service back-end.
In general you don't want to bother users with signed documents, they just don't care.
Users simply want to use automated workflows, and the creation/validation of digital signatures can be part of.
The service of course keeps record of the signed documents (in case a legal dispute should arise in the future), but everything -including the signature validation- should be part of the automated workflow as offered by the business web application.

## Service Directive readiness ##

## Long-Term Accessibility ##

## Long-Term Signature Validity ##

## Risk for Vendor lock-in ##

## Preservation of Visualization ##

## Visualization of Signatures ##

Another interesting subject is the 'human-readable' aspect of a signature.
There seems to be an interesting mind-twist: some say that PDF is more human-readable than XML. I noticed the same mind-twist when working on the TSL. There I also witnessed the fact that people think of PDF as being human-readable, while XML is being machine-readable.

However, when you open a XAdES-X-L signed XML file in notepad, and you do the same for PDF, the XML version is actually easier to read than the PDF version.

In my view PDF is not human-readable, the output of a PDF reader is human-readable, but the PDF format itself carries some serious vendor locking risk.