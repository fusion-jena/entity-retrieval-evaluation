#
# gets related entites from terminology service for pre-defined BEF-China queries with entities
# input: expansion type, sparql query, path to output folder
#  
# output: per query and entity a file with related entities is generated
#
# @author: Felicitas Loeffler, 2021

# import regex library
import re
import requests
import os
import datetime
import argparse
# path to folder with query files
root = 'entities'


#method for getting the command line arguments
def commandLine():
	print("read command line")

	#path to output folder
	global pathToOutput
	#expansion keyword
	global expansion
	#root path
	global pathToRoot

	try:
		#command line arguments
		parser = argparse.ArgumentParser(description="run a sparql query on BioCADDIE entities")
		parser.add_argument("-e", "--expansion", help="Set keyword of expansion (broader | coreRelation)", required=True)
		parser.add_argument("-r", "--root", help="Set root folder, e.g., bioCADDIE or BEFChina", required=True)    
		parser.add_argument("-o", "--output", help="Set path to output folder, e.g., broader or coreRelations", required=True)
		args = parser.parse_args()

		pathToOutput = args.output
		pathToRoot = args.root
		expansion = args.expansion
		
	except:
		raise Exception("")


def create_file(request_result, file, entity1, entity2):
	#print('j: ',+j)
	# open the file
	filename = file.name.split('.')
	#print(filename[0])
	entity1 = entity1.replace('<','')
	if len(entity2) >0 :
		entity2 = entity2.replace('<','')
	entity1 = entity1.replace('>','')
	if len(entity2) >0 :
		entity2 = entity2.replace('>','')
	entity1 = re.sub(r"[\n\t]*", "", entity1)
	if len(entity2) >0 :
		entity2 = re.sub(r"[\n\t]*", "", entity2)
	entity1 = entity1.split('/')
	if len(entity2) >0 :
		entity2 = entity2.split('/')
	#print(entity)
	if len(entity2) >0 :
		file = open(pathToRoot+'/'+pathToOutput+'/'+filename[0]+'_'+entity1[len(entity1)-1]+'_'+entity2[len(entity2)-1]+'.txt', 'a')
	else:
		file = open(pathToRoot+'/'+pathToOutput+'/'+filename[0]+'_'+entity1[len(entity1)-1]+'.txt', 'a')
	file.write(request_result)
	file.close()


def getRelation(pathToSparql):
	countFiles = 0
	query = open(pathToSparql).read()
	#print(pathToSparql)
	with os.scandir(pathToRoot+'/'+'entities') as entries:
		for file in entries:
			if(file):
				print(file)
				print(file.name)
				countFiles = countFiles + 1
				# open file
				entityFile = open(file, "r")
				# for each line in the sparql file
				lines = entityFile.readlines()
				for i,line in enumerate(lines):
					entity1 = line
					URIarray = line.split('/');
					graph = URIarray[len(URIarray)-1].split('_');
					#print(line)
					fullQuery = query.replace('?entity1', entity1)
					fullQuery = fullQuery.replace('?g', '<http://gfbio-git.inf-bb.uni-jena.de/BIODIV/'+graph[0].upper()+">")
					#print(fullQuery)
					params = {'query': fullQuery}
					url = 'http://gfbio-git.inf-bb.uni-jena.de/graphDB/repositories/BIODIV'
					request_result = requests.post(url, params).text
					#result = request_result.replace('\n','')
					#if no result is returned the request.text contains 'property,object  ' (17 characters)
					#print(len(request_result))
					if(len(request_result)>8): # ?+ propertyLable = 22
						create_file(request_result, file, entity1, "")
					if(len(request_result)==147):
						print(fullQuery) 
						print('entity1:' + entity1)					   
				# close the file
				entityFile.close()

			
#main method
if __name__ == '__main__':
	try:
		now = datetime.datetime.now()
		print("\nProgram started " + str(now) + ".\n")
		commandLine()
		if expansion == "coreRelation":
			getRelation(pathToRoot+'/'+'sparqlRelationsROCORE.sparql')
		elif expansion == "broader":
			getRelation(pathToRoot+'/'+'sparqlBroader.sparql')
		print("\nquery processed " + str(now) + ", result saved in "+pathToRoot+'/'+pathToOutput+".\n")
		print("\nProgram ended " + str(now) + ".\n")
	except Exception as ex:
		print(ex)

