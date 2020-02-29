# Geolocalisation Data Challenge

## Objective

A company that produces connected device needs to locate the position of their device. They could use GPS but using prediction based on data seems cheaper.
Prediction is based on the message reception information.

## Inputs

Inputs are different information about messages:

![InputImage](https://github.com/savoga/various_projects/blob/master/Data_Challenges/Geolocalisation/Inputs.png)

Note: _rssi_ (Received Signal Strength Indicator) is an estimation of the signal power level received by the device.

Note: we won't use column _nseq_ and _time_ux_.

## Outputs

Outputs are the localisation (latitude and longitude) of the located device:

![OutputImage](https://github.com/savoga/various_projects/blob/master/Data_Challenges/Geolocalisation/output.png)

## Materials

For this challenge, we have:
- Training samples and their corresponding predictions
- Test samples for which we need to produce predictions associated

## Project structure
<!-- TOC -->
- [LOAD DATA](#load data)
- [DATA EXPLORATION](#data-exploration)
- [PREPROCESSING](#preprocessing)
- [PREDICTION](#prediction)
	- [LINEAR REGRESSION](#reduce)
		-[CROSS VALIDATION](#cross-validation)
	- [RANDOM FORESTS](#reduce)
		-[CROSS VALIDATION - LEAVE ONE DEVICE OUT](#cross-validation---leave-one-device-out)
- [POSTPROCESSING](#postprocessing)
<!-- /TOC -->

## Load data

Three dataframes are used:
- training samples
- their corresponding predictions
- test samples

## Data exploration

We first display device (messages) and bases on a map. We notice some bases are very far away from the device, which are outliers.
We then decide to display the same map without outliers. Arbitrary distances have been choosen: lattitude between 43 and 65 and longitudes between -65 and -104.
