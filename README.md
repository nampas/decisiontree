decisiontree
============
A decision tree for classifying arbitrary data models. A less generic version of this project was submitted to fulfill an assignment for Adam A. Smith's CS431 (Artificial Intelligence) class at the University of Puget Sound.  
The decision tree requires a ```DataModel``` to be instantiated. A ```DataModel``` can be constructed programatically by calling ```addDatum()``` on its builder class for each datum the user wishes to add to the data set. Alternately, the user can supply a .tsv file of data, where a line might look the following:
```
Rep-17	D	++++-+-++-
```
The first token of each data entry contains a unique identifier, the second token contains that datum's label, and the third token is a string of feature values. In this case, ```Rep-17``` is the unique identifier, ```D``` is the datum's label, and ```++++-+-++-``` is a string of 10 features values.
Files following this format can be parsed by ```VotingTester.parseFile()```.
