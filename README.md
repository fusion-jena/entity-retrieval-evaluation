# Entity Retrieval Evaluation

This repository provides the code of experiments for entity based retrieval taking semantic relations of entities into account.

We utilized the following implemented scorer from [GATE Mímir](https://github.com/GateNLP/mimir/):

* BM25
* TF-IDF (CF-IDF)

As we utilized an annotation index with URIs, both ranking algorithms already work concept based and not term based.

We added a variation of CF-IDF (called CF-IDF exact) to only consider hits with URIs occurring in the query. This gives more credit to matches with URIs in case the entity linking did not work properly and could only extract a semantic type but not a concept in an ontology.

In addition, we implemented the following entity based retrieval models:

* [HCF-IDF by Nishioka and Scherp, 2016](https://doi.org/10.1145/2910896.2910898)
* [Taxonomic Scorer by Waitelonis, Exeler and Sack, 2015](https://ceur-ws.org/Vol-1581/)

For the node level computation in HCF-IDF, we utilized [slib](https://github.com/sharispe/slib) with a slight extension (see descriptions below):

## Overview of the available software code

* [SML extension](https://github.com/MetadataRetrieval/code/tree/main/slib-sml/src/main/java/slib/sml/sm/core/engine)(node level computation for HCF-IDF)

* [slibAPI](https://github.com/MetadataRetrieval/code/tree/main/slibAPI) (Wrapper)

* [GATE Mimír plugins](https://github.com/MetadataRetrieval/code/tree/main/Mimir6.2)

* [Mímir Search API](https://github.com/MetadataRetrieval/code/tree/main/MimirSearchAPI) (Wrapper)

* [Mímir Test](https://github.com/MetadataRetrieval/code/tree/main/MimirTest/mimirTest)

* [BiodivTagger]((https://github.com/MetadataRetrieval/code/tree/main/BiodivTagger))

## Installation instructions for entity linking in metadata

prerequisites: The following instructions have been tested with Java 8 and [GATE version 8.6.1](https://gate.ac.uk/). Please download and install GATE 8.6. Please also download, unzip and load the [Ontology plugin](https://github.com/GateNLP/gateplugin-Ontology/releases/tag/v8.5). To load the plugin, start GATE and open the Plugin Manager (the jigsaw icon) in the menu. Then select "open from folder" and navigate to the folder with the Ontology plugin.

#### Load the extended BiodivTagger pipeline

To load a new application in the GATE UI, do a right-click on "Application" and "Restore Application from file". Then navigate to the application.xgapp file of the BEFCHina or BioCADDIE folder. Create a new corpus and add a metadata file you want to annotate, e.g., download the [BEFChina](https://bef-china.com/) datasets and annotate them (run the pipeline). 


## Installation instructions for metadata indexing, search and tests

prerequisites: The following instructions have been tested with Java 8 and Maven 3.3.9


#### 1.) SML library 0.9.5

Download the [slib](https://github.com/sharispe/slib) project. Further information about semantic similarity measures implemented in slib can be found on the webpage: http://www.semantic-measures-library.org/

In order to compute the node level for the HCF-IDF we adjusted the Semantic Measures Engine in the slib-sml subproject. As there is no plugin API in slib, add the methods given in  ``slib.sml.sm.core.engine.SM_engine`` to the respective file in slib/sml.

Build and install the SML library with Maven.

#### 2.) slibAPI and used ontologies

The slibAPI is wrapper project for the SML library and loads all ontologies as external knowledge sources. Download and install the slibAPI. Create a ``resource/vocabs`` folder under the root folder ``slibAPI``. Download the following ontologies in owl format from [OBO Foundry](http://www.obofoundry.org/) and store it in the ``vocabs`` folder: 

* [bfo](http://www.obofoundry.org/ontology/bfo.html)
* [bto](http://www.obofoundry.org/ontology/bto.html)
* [chebi](http://www.obofoundry.org/ontology/chebi.html)
* [cl](http://www.obofoundry.org/ontology/cl.html)
* [doid](http://www.obofoundry.org/ontology/doid.html)
* [envo](http://www.obofoundry.org/ontology/envo.html)
* [flopo](http://www.obofoundry.org/ontology/flopo.html)
* [go](http://www.obofoundry.org/ontology/go.html)
* [hp](http://www.obofoundry.org/ontology/hp.html)
* [ino](http://www.obofoundry.org/ontology/ino.html) 
* [mod](http://www.obofoundry.org/ontology/mod.html)
* [ncbitaxon](http://www.obofoundry.org/ontology/ncbitaxon.html)
* [ncit](http://www.obofoundry.org/ontology/ncit.html)
* [obi](http://www.obofoundry.org/ontology/obi.html)
* [pato](http://www.obofoundry.org/ontology/pato.html)
* [po](http://www.obofoundry.org/ontology/po.html)
* [ppo](http://www.obofoundry.org/ontology/ppo.html)
* [rex](http://www.obofoundry.org/ontology/rex.html)
* [symp](http://www.obofoundry.org/ontology/symp.html)
* [to](http://www.obofoundry.org/ontology/to.html)
* [uberon](http://www.obofoundry.org/ontology/uberon.html) (core ontology - uberon.owl + Uberon extended - ext.owl)

Build and install the slibAPI project.

#### 3.) GATE Mímir

Download and install [GATE Mímir version 6.2](https://github.com/GateNLP/mimir/releases). Further information on the requirements of GATE Mímir and a user guide can be found on their webpage: https://gate.ac.uk/mimir/.

Install the scorer plugins provided in the ``GATE Mímir/plugins`` folder as described in [Mímir's user guide](https://gate.ac.uk/mimir/doc/mimir-guide.pdf). 

All ontologies needs to be loaded during Mímir's start. Therefore, initialize the  slibAPI/SML class in the MimirScorerService. Have a look at the provided file in the ``Mimir6.2/webapp`` folder 

Now index a corpus with GATE Mímir, e.g., download the [BEFChina](https://bef-china.com/) datasets and annotate them with the [OrganismTagger](http://dx.doi.org/10.1093/bioinformatics/btr452) and the [BiodivTagger](https://aclanthology.org/2020.lrec-1.560.pdf).
For testing purposes, one can also use any text files and annotate them with the default [GATE ANNIE](https://gate.ac.uk/ie/annie.html) pipeline (extracts e.g., Location, Person, Date and Time).

We provide an index template for the annotations obtained from the OrganismTagger and BiodivTagger.

#### 4.) Mímir Search API

The Mímir Search API project is a wrapper for Mímir and provides more convenient access for developers to the Mímir's search.
Download and install the Mímir search API project. In the ``MimirSearch.java`` file adjust the index URL.

#### 5.) Mímir Test

The MimirTest project provides the code we ran to evaluate entity expansion and entity-based ranking functions on two test collections. It can be also used as example code for own search purposes and evaluations. 

## TREC evaluation

The evaluation results are available at Zenodo: [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.7396782.svg)](https://doi.org/10.5281/zenodo.7396782)

## License
The code in this project is distributed under the terms of the [GNU LGPL v3.0.](https://www.gnu.org/licenses/lgpl-3.0.en.html)
