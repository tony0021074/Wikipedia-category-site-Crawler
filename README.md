# Wikipedia category site Crawler (school individual assignment)

The application is developed with IDE Intellij IDEA in Java programming language (Java 8 SE). It has a graphical user interface using JavaFX platform. The application has been run normally in Windows environment.\
It is a web spider programme that is specialized to crawl Wikipedia category site. For the pages under categories, they are downloaded and saved as html files. It supports two searching methodology, breadth-first search (BFS) and depth-first search (DFS).

## User Manual:

Basic control:
1. Enter a Wikipedia category site's absolute URL. (e.g. https://en.wikipedia.org/wiki/Category:Technology, https://en.wikipedia.org/wiki/Category:Lists_of_television_channels)

2. Start crawling by clicking Start button. Folders and html files are created under predefined download folder. Some controls are disabled.

3. When crawling is finished, the program's enable the disabled control automatically.

Advanced control:
- Stop the crawling during crawling by clicking the Stop button. Continue the crawling by click the Start button

- The default download directory is inside the program's files. Choose a folder for storing the folders and html files. 

- Please choose an empty download directory. Otherwise the existing files are shown in tree view and even be modified or deleted.

- Open the folder containing the .html file by double clicking it in the tree view.
