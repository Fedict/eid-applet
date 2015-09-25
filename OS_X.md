# Introduction #

OS X and Java have never been a good marriage. This page tries to help Apple users in getting their environment ok for OS X.

# Details #

## OS X 10.6.7 ##

The ccid driver on this version is only compiled for running in 32 bit mode and not in both 64 and 32 bit mode as the pcsc daemon is. Hence that 64 bit browsers like Firefox 4 and Safari will give problems for the eid applet.

### Java 1.6.0\_24-b07-334 ###

**Firefox 3.6**

eid authentication and identification of the applet should work out of the box as 3.6 is a 32 bit browser.

**Firefox 4** and **Chrome 10** and **Safari 5**

Will not work for any operation as this is a 64 bit browser. This can be resolved with following "Java Preferences" settings:
  * Run applets: In their own process
  * Java SE6 32bit on top of 64bit in ordering

![http://eid-applet.googlecode.com/svn/wiki/images/ff4.png](http://eid-applet.googlecode.com/svn/wiki/images/ff4.png)

Using this setting should allow for all operations to work.

**Known Issues**

Some display issues like progressbar flickering exist on non-webkit browsers (Firefox, Chrome ) at the moment.  This due to the latest Java update which is using the new Java Browser Plugin 2 which still has alot of bugs. WebKit browsers like Safari and Opera do not have these issues as they are using a custom java plugin, built by Apple.
Some info regarding these issues can be found at:
[Apple/Java Discussion](https://discussions.apple.com/message/13296008?messageID=13296008%25EF%25BF%25BD)

When using the sign operation, a dialog from the eid middleware will not disappear after the sign operation has finished. This does not cause any failure on that or future signing operations. Work is in progress on this issue and can be found [here](http://code.google.com/p/eid-mw/issues/detail?id=65)


### Java 1.6.0\_25-b06-361 ###

This (currently only in developer preview) update has fixed a lot of issues for the Plugin2 Java Browser Plugin, like the flickering issues on non-webkit browsers.

It also fixes the issue of the 64bit JVM not being able to communicate correctly with the 32bit pcscd. ( which can run in both 64bit as 32bit but switches as the 2 ccid drivers are both only compiled 32bit ...).
So basically everything works out of the box without any explicit configuration with this developer preview, lets hope for a quick official release by apple.

The preview can be found at: https://connect.apple.com/cgi-bin/WebObjects/MemberSite.woa/wa/getSoftware?bundleID=20809


## OS X 10.7 aka Lion ##

Upon first use of Java, OS X will install version 1.6.0\_26-b03-383. This version has the above fixes in it so nothing special needs to be done from now on.
Beware that also the first time a Java applet will be run the java browser plugin will need to be explicitly activated, needing a restart of your browser. After that, all should be good.

Tested on Chrome 12.0.742.122, Firefox 5.0.1 and Safari 5.1 (7534.48.3).