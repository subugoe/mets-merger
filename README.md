SUB METS Merger
============

Generating the documentation
----------------------------
> mvn site

Results will be in ./target/site. Just open ./target/site/index.html in your browser.

Creating a deployable file
--------------------------
> mvn package

Results will be in ./target.

Running the File
----------------
> java -jar mets-merger.jar
