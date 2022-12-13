# Entity Expansion

The following scripts fetch related entities with SPARQL queries and add them to the original search questions.

1. detectRelations.py: retrieves related entities with sparql queries
2. constructQuestionsForEvaluation.py: generates the final entity expanded queries for the entity retrieval evaluation

## Prerequisites

Tested and developed with Python 3.9

## Example calls

```python detectRelations.py -e broader -r bioCADDIE -o relation```

```python bioCaddie/constructQuestionsForEvaluation.py```
