GrokView
========

A graphical browser for [OpenGrok][1] servers, based on the OGRE library.

This application allows the user to place a query with an OpenGrok server,
retrieve the results, and view all file and line matches in a directory tree.

[1]: http://hub.opensolaris.org/bin/view/Project+opengrok/


**Notice:** This application can put a high, atypical load on servers that might be
dimensioned for human readers browsing only a small part of the results. Users are
therefore *strongly discouraged* from accessing a site with this program without
the consent of the administrator.

In particular, do not simply enter a public source code search server and start
browsing away!


Dependencies
------------

This application depends on the following libraries:

 *  OGRE (OpenGrok Retrieval Engine):  
    [https://github.com/sebako/ogre/tree/master/ogre]()
    
 *  Swing Plus:  
    [https://github.com/sebako/swingplus]()