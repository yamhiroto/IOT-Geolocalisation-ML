# -*- coding: utf-8 -*-

"""
15/10/2019
1) Extract list of top contributors
2) Get the mean of all their repos' stars
3) Sort contributors by this last number


Récupérer via crawling la liste des 256 top contributors sur cette page https://gist.github.com/paulmillr/2657075
En utilisant l'API github (https://developer.github.com/v3/) récupérer pour chacun de ces users le nombre moyens 
de stars des repositories qui leur appartiennent. Pour finir, classer ces 256 contributors par leur note moyenne.﻿

Comme l'API github dispose de restrictions d'accès (limitation du nombre de requêtes), vous aurez besoin de vous 
authentifier via un token: 
https://help.github.com/en/articles/creating-a-personal-access-token-for-the-command-line.

"""

import requests
from bs4 import BeautifulSoup
import operator

def getMeanStars(repos):
    stars = 0
    count = 0
    star_dict = {}
    star_list = []
    for repo in repos:
        stars+=repo['stargazers_count']
        count+=1
        star_dict[repo['name']]=repo['stargazers_count'] # if needed
        star_list.append(repo['stargazers_count']) # if needed
    return stars/count

def getRepos(user):
    API_TOKEN='47ad9d169977d1894d63cb7a5b0cf297d23d5e90'
    GIT_API_URL='https://api.github.com'
    REPO_LIMIT=5
    headers = {'Authorization': 'token ' + API_TOKEN}
    return requests.get(GIT_API_URL+'/users/'+user+'/repos'+'?per_page='+str(REPO_LIMIT), headers=headers).json()

website = "https://gist.github.com/paulmillr/2657075"

response = requests.get(website).text
soup = BeautifulSoup(response)

starMeans_dict = {}

for row in soup.findAll('th', {'scope': 'row'}):
    contributor = row.findNext('a').string
    repos = getRepos(contributor)
    if(len(repos)==0):
        print(("User {} has no repo!").format(contributor))
        continue
    print("{} repos found for {}".format(len(repos),contributor))
    meanContributor = getMeanStars(repos)
    starMeans_dict[contributor]=meanContributor
    print("user {} has a mean of {}".format(contributor, meanContributor))
contributors_sorted = sorted(starMeans_dict.items(), key=operator.itemgetter(1), reverse=True)
