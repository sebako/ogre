OGRE
====

OpenGrok Retrieval Engine


Overview
--------

This library allows programmatic access to [OpenGrok][1] servers. Since OpenGrok
provides no web service API, the HTML output returned by the OpenGrok web server
is parsed, and Java objects representing file and line matches are returned to
the caller.

**Notice:** This library can put a high, atypical load on servers that might be
dimensioned for human readers browsing only a small part of the results. Users are
therefore *strongly discouraged* from accessing a site with this library without
the consent of the administrator.

[1]: http://hub.opensolaris.org/bin/view/Project+opengrok/


Dependencies
------------

This library has no external dependencies.