#
# writes sparql queries into specific files
# input: folder with sparql queries, one query per file, per file (query) 4 different expansion strategies
#  
# output: sparql queries per expansion strategie
#
# @author: Felicitas Loeffler, 2020

# import regex library
import re
import datetime
import os

# path to folder with query files
files= ['query1.sparql', 'query2.sparql', 'query3.sparql', 'query4.sparql', 'query5.sparql', 'query6.sparql', 'query7.sparql', 'query8.sparql', 'query9.sparql', 'query10.sparql', 'query11.sparql', 'query12.sparql', 'query13.sparql', 'query14.sparql']

# output folder containing the sparql queries per expansion type
outputPath = 'expanded/'

# pattern for inst annotations with inst queries
URIRegex = "### simple URI query"

# general pattern for a category with an instance
instPattern = "({(Organism|Process|Quality|Environment|Material)\s?inst\s?=\s?\"(http://purl.obolibrary.org/obo/[A-Za-z]+_[A-Z0-9]+)\"})"

#coreRelation keyword
coreRelation = "coreRelation"

#stop pattern
stopPattern = "###+"

def remove_last_line_from_string(s):
	return s[:s.rfind('#+')]

def create_file(expansion, j, lines):
	#print('j: ',+j)
	# open the file
	file = open(outputPath+expansion+'.txt', 'a')
	foundSparql = True
	# when broader is found - three lines later the sparql query starts
	query = ""
	while (foundSparql):
		# loop forward as long as you can not find the stop pattern
		stop = re.search(stopPattern, query)
		if stop:
			# remove the ##### line
			#print (query)
			query = remove_last_line_from_string(query)
			
			# remove the tabs and newline breaks, we want the query in one line
			query = re.sub(r"[\n\t]*", "", query)
			query = re.sub(r"#+", "", query)
			#print (expansion +" query: ", query)
			# write the final query to the file
			file.write(query)
			file.write('\n')
			# and stop
			foundSparql = False
			#print ('foundSparql:', False)
		else:
			#print (query)
			# append all query parts you can find
			query = query + lines[j] + " "
			# analyze the next line
			#if (len(lines) > j):
			j = j+1
	file.close()

def create_expansion_file(expansion):
	#print('j: ',+j)
	# open the file
	outputFile = open(outputPath+expansion+'.txt', 'a')
	inputFile = open(outputPath+'URI.txt', 'r')
	lines = inputFile.readlines()
	for i,line in enumerate(lines): 
		# when matching the inst pattern
		#print(line)
		instArray = re.findall(instPattern, line)
		#print("Line" + str(i))
		for j in instArray:
			#j[0] - entire match, e.g.: {Anatomy inst="http://purl.obolibrary.org/obo/NCIT_C12453"}
			#j[1] - category, e.g.: Anatomy
			#j[2] - URI, e.g: http://purl.obolibrary.org/obo/NCIT_C12453
			#print(j[0])
			category = j[1]
			graphArray = j[2].split('/')
			graph = graphArray[len(graphArray)-1].split('_');
			if expansion == 'narrower':
				line = re.sub(j[0], '('+j[0] + ' OR {'+category+' broader=\"http://purl.obolibrary.org/obo/'+graph[0]+'_'+graph[1]+'\"})',line,count=1)
			else:
				try:
					path = expansion+'/query'+str(i+1)+'_entities_'+graph[0]+'_'+graph[1]+'.txt'
					#print(path)
					expansionFile = os.path.isfile(path)
					if expansionFile and expansion == 'coreRelation':
						replacement = getExpansion(i+1,expansion,j[1],"inst",graph[0],graph[1])
						print("Replacement: "+ replacement)
						line = re.sub(j[0],replacement,line,count=1)
					elif expansionFile and expansion == 'broader':
						replacement = getExpansion(i+1,expansion,j[1],"broader",graph[0],graph[1])
						# replace it only if broader terms could be found
						if len(replacement) >0:
							line = re.sub(j[0],'('+j[0] + ' OR ' + replacement + ')',line,count=1)
				except:
					print("no file found for "+ j[0])
		outputFile.write(line)
	outputFile.close()
	inputFile.close()

def getExpansion(i,expansion,category,inst,graph,graphID):
	if expansion == 'broader':
		replacement = '('
	else:
		replacement = "({"+category+" "+inst+"=\"http://purl.obolibrary.org/obo/"+graph+"_"+graphID+"\"}"
	try:
		file = open(expansion+'/query'+str(i)+'_entities_'+graph+'_'+graphID+'.txt', 'r')
		lines = file.readlines()
		count = 0
		for j,fileLine in enumerate(lines): 
			fileLine = fileLine.replace('\n', '')
			#jump over first line --> only "object"
			if count < 1 or fileLine == '' or fileLine.startswith('_:node') or fileLine == 'broader':
				count = count + 1 # just go on, nothing to do here
			else:
				length = len(replacement)
				if length > 2:
					replacement = replacement + " OR "
				replacement = replacement + "{"+category+" "+inst+"=\""+fileLine+"\"}"
				count = count + 1
		file.close()
	except:
		print("no file found - simple URI replacement")
		#replacement = "{"+category+" "+inst+"=\"http://purl.obolibrary.org/obo/"+graph+"_"+graphID+"\"}"
	if replacement == '(':
		replacement = ""
	else:
		replacement = replacement + ")"
	return replacement


def createURIfile():
	countFiles = 0

	for file in files:
		print(file)
		countFiles = countFiles + 1
		# open file
		#sparqlFile = open(os.path.join(subdir, file), "r")
		sparqlFile = open(file, "r")
		# for each line in the sparql file
		lines = sparqlFile.readlines()
		#print('lines length: ', len(lines))
		noURIFound = True
		for i,line in enumerate(lines): 
			# when matching the narrower pattern
			URI = re.search(URIRegex, line)
			if URI:	 
				noURIFound=False
				j = i+3
				create_file("URI",j, lines)
		# if no URI statement could be found - default entry with query number
		if noURIFound:
			file = open(outputPath+"URI"+'.txt', 'a')
			query = "#Q"+ str(countFiles)
			file.write(query)
			file.write('\n')
			file.close()
		# close the file
		sparqlFile.close()


#main method
if __name__ == '__main__':
	try:
		now = datetime.datetime.now()
		print("\nProgram started " + str(now) + ".\n")
		#at first create the simple URI files - no expansion
		createURIfile()
		# create the "narrower" expansion file
		create_expansion_file("narrower")
		# create the "broader" expansion file
		create_expansion_file("broader")
		# create the "coreRelations" expansion file
		create_expansion_file(coreRelation)
	except Exception as ex:
		print(ex)




