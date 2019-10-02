# -*- coding: utf-8 -*-

import requests
from bs4 import BeautifulSoup
import re
from difflib import SequenceMatcher
import time

def similar(a, b):
    return SequenceMatcher(None, a, b).ratio()

def getFirstLink(website):

    response = requests.get(website).text
    soup = BeautifulSoup(response)

    ### RETRIEVE ARTICLE NAME
    title = soup.title.string.split(" Wiki",1)[0]
    title = re.sub(r'\([^)]*\)', '', title)

    ### SEARCH FOR THE STARTING WORD (IN BOLD)
    soup_main_bloc = soup.find('div', {'class': 'mw-parser-output'})

    k = 1
    startWord = ""
    for boldWord in soup_main_bloc.findAll('b'):
        parentWord = str(boldWord.parent)
        if("<p>" in str(boldWord.parent)):
            word = str(boldWord.string)
            if(similar(word,title) > 0.5):
                startWord = "<b>"+word+"</b>"+parentWord.split("<b>" + word + "</b>",1)[1][0:5]
                break
        k+=1

    ### RETRIEVE LINKS FROM STARTING WORD
    if(startWord == ""):
        raise Exception("Program didn't find the startWord!!!!!")
        
    #sep = "<b>" + startWord + "</b>"
    new_response = response.split(startWord, 1)[1]
    
    #new_response=parentWord

    new_soup = BeautifulSoup(new_response)
    links = []

    for link in new_soup.findAll('a', attrs={'href': re.compile("wiki/")}):
        links.append(link.get('href'))
    
    if(len(links)==0):
        raise Exception("No links found!!!!!")

    return links[0]


site = "https://fr.wikipedia.org/wiki/Anglais"

#print(getFirstLink(site))


site = site.replace("https://fr.wikipedia.org","")

allFirstLinks = []

count = 1
for i in range(20):
    if("hilosoph" in site):
        print("Distance to philosophy: " + str(count))
        break
    site = "https://fr.wikipedia.org" + site
    print(site)
    site = getFirstLink(site)
    allFirstLinks.append(site)
    time.sleep(0.1)
    count+=1

print(allFirstLinks)