# -*- coding: utf-8 -*-

import requests
from bs4 import BeautifulSoup
import re
from difflib import SequenceMatcher
import time

"""
03/10/2019:
"Every Wikipedia article leads to the topic of philosophy"
(https://secouchermoinsbete.fr/69863-tous-les-chemins-menent-a-la-philosophie-sur-wikipedia)
This algorithm retrieved the distance from any wikipedia article to a philosophic article on Wikipedia
"""

def similar(a, b):
    return SequenceMatcher(None, a, b).ratio()

def getFirstLink(website):

    response = requests.get(website).text
    soup = BeautifulSoup(response)

    ### RETRIEVE ARTICLE NAME
    title = soup.title.string.split(" Wiki",1)[0]
    title = re.sub(r'\([^)]*\)', '', title).replace(" —","")

    ### SEARCH FOR THE START WORD (IN BOLD)
    soup_main_bloc = soup.find('div', {'class': 'mw-parser-output'})

    startWord = ""
    listOfAllBoldWords = soup_main_bloc.findAll('b')
    for boldWord in listOfAllBoldWords:
        parentWord = str(boldWord.parent)
        # When the bold word is also in italic, we have to get one more parent level
        if("<i><b>" in parentWord):
            parentWord = str(boldWord.parent.parent)
        # When the parent has <p> --> we are most likely in the first paragraph
        if("<p>" in parentWord):
            word = str(boldWord.string)
            if(similar(word,title) > 0.7):
                startWord = "<b>"+word+"</b>"+parentWord.split("<b>" + word + "</b>",1)[1][0:5]
                break

    ### RETRIEVE LINKS FROM START WORD
    if(startWord == ""):
        raise Exception("Program didn't find the startWord!!!!!")
        
    new_response = response.split(startWord, 1)[1]

    new_soup = BeautifulSoup(new_response)
    links = []

    for link in new_soup.findAll('a', attrs={'href': re.compile("wiki/")}):
        links.append(link.get('href'))
    
    if(len(links)==0):
        raise Exception("No links found!!!!!")

    return links[0]


site = "https://fr.wikipedia.org/wiki/Anglais"

site = site.replace("https://fr.wikipedia.org","")

allFirstLinks = []

count = 1
for i in range(20):
    if("hilosoph" in site):
        print("//******* Distance to philosophy: %d ********\\" %(count))
        break
    site = "https://fr.wikipedia.org" + site
    print(site)
    site = getFirstLink(site)
    allFirstLinks.append(site)
    time.sleep(0.1)
    count+=1