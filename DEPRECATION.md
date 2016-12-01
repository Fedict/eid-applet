# History

The eID applet project was launched in 2009 to facilitate the use of Belgian eID cards in web applications. 
At that time there were few alternatives to do this. 
Because the eID applet is independent of the eID middleware, it was enough to install a JRE (Java Runtime Environment).

It was decided to opt for an Open Source licence (GPL v. 3), which made it possible for all developers to use the code. 
Binary versions were also devised (signed by Fedict) for use on Belgian websites (.be).

The first stable release followed in 2010. The code was subsequently further developed (including support for new operating systems, card readers and Java Runtime). 
Later, this was followed by further projects which continued to build on the eID applet: IDP for authentication and DSS for placing digital signatures.

# Functionality

The functionality of the eID applet can be subdivided into three blocks:
- Obtaining the identity information on the eID card, including name, address and photo.
- Authentication using the authentication certificate on the eID card.
- Placing digital signatures using the signature certificate on the eID card.

# Problems surrounding the eID applet

In the course of the years a number of issues have arisen that make the eID applet (and the eID card in general) more difficult to use in web applications:/
- The use of the Java plug-in in the browser is not recommended. This plug-in uses an old API (NPAPI) which was created by Netscape at that time. In fact, in a number of browsers, including Chrome and Edge, support for it has been dropped.
- The next version of Java (Java 9) will no longer offer support for applets.
- Desktop Java has a bad reputation as a result of a number of security problems. Many people are wary about installing this.

# Alternatives

There is no problem using the eID card for authentication. 
The client certificate on the eID card can be used to set up a TLS connection with mutual authentication. 
However, the eID middleware must be installed for this.


Fedict offers no alternative for the other applications. 
However, there are increasing numbers of players on the market who are developing an alternative themselves, usually under a commercial licence. 
Usually, this consists of a browser extension that talks with a local process that addresses the eID card.