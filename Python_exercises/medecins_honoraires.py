#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Nov  5 12:49:45 2019

@author: savoga
"""

import pandas as pd
from difflib import SequenceMatcher

def similar(a, b):
    return SequenceMatcher(None, a, b).ratio()

def findCloserMatchVector(col_1, col_2, deg):
    a={}
    for spe_1 in col_1:
        for spe_2 in col_2:
            if(similar(spe_1,spe_2)>deg):
                a[spe_1]=spe_2
        if(spe_1 not in a):
            a[spe_1]=""
    return a

# Following function looks more elegant however it takes much more time to compute
#def findCloserWord(word_0):
#    words = list(df_2['specialites'].unique())
#    deg_simil_min = 0.6
#    deg_simil_max = deg_simil_min
#    closestWord = ""
#    for word in words:
#        deg_simil_temp = similar(word_0, word)
#        if(deg_simil_temp > deg_simil_max):
#                closestWord=word
#                deg_simil_max = deg_simil_temp
#    return closestWord
#
#data = df_1['l_pre_spe'].apply(findCloserWord)


# Dépassements/spécialité
df_1 = pd.read_csv("/home/savoga/Documents/Données_santé/n201808.csv", delimiter=";", encoding = "ISO-8859-1")
df_1 = df_1[['l_pre_spe', 'dep_mon']]

# Nombre de médecins/spécialité
df_2 = pd.read_excel("/home/savoga/Documents/Données_santé/medecins_specialites.xls")
df_2 = df_2[8:]
df_2 = df_2.rename(columns={"TABLEAU 1. EFFECTIFS DES MÉDECINS par spécialité, mode d'exercice, sexe et tranche d'âge": "specialites"})
df_2 = df_2.rename(columns = {"Unnamed: 1": "effectif"})
df_2 = df_2[['specialites','effectif']]
df_2['effectif'] = df_2['effectif'].astype(float)

a=findCloserMatchVector(list(df_1['l_pre_spe'].unique()), df_2['specialites'], 0.7)
a["02-Anesthésiologie - Réanimation chirurgicale"]="Anesthésie-réanimation"
a["03-Pathologie cardio-vasculaire"]="Cardiologie et maladies vasculaires"
a["18-Stomatologie"]="Chirurgie maxillo-faciale et stomatologie"
a["33-Psychiatrie générale"]="Psychiatrie"
a["38-Médecin biologiste"]="Biologie médicale"
a["42-Endocrinologie"]="Endocrinologie et métabolisme"
a["74-Oncologie radiothérapique"]="Oncologie option médicale"
a["77-Obstétrique"]="Gynécologie-obstétrique"

df_1['spe_2']=df_1['l_pre_spe'].apply(lambda x: a[x])
df_1 = df_1[df_1.spe_2 != ''].reset_index(drop=True)
df_1['dep_mon'] = df_1['dep_mon'].str.replace(',','.')
df_1['dep_mon'] = df_1['dep_mon'].apply(lambda x: x[0:len(x)-3].replace('.','') if len(x)>4 else x)
df_1['dep_mon']=df_1['dep_mon'].astype(float)
df_1 = df_1.groupby(['spe_2'])['dep_mon'].agg('sum')
df_1=df_1.reset_index()

df_3 = df_1.join(df_2.set_index('specialites'), on='spe_2')
df_3.plot(x='spe_2', y='dep_mon', kind='bar', legend=False, figsize=(15, 10))
df_3.plot(x='spe_2', y='effectif', kind='bar', legend=False, figsize=(15, 10))
print("la correlation entre les effectifs et le dépassement monétaire est {}".format(df_3['effectif'].corr(df_3['dep_mon'])))