# Geolocalisation Data Challenge

## Objective

A company that produces connected device needs to locate the position of their device. They could use GPS but using prediction based on data seems cheaper.
Prediction is based on the message reception information.

## Input

Inputs are different information about messages:

![InputImage](https://github.com/savoga/various_projects/blob/master/Data_Challenges/Geolocalisation/Inputs.png)

Note: _rssi_ (Received Signal Strength Indicator) is an estimation of the signal power level received by the device.

Note: we won't use column _nseq_ and _time_ux_.

## Output

Outputs are the localisation (latitude and longitude) of the located device:

![OutputImage](https://github.com/savoga/various_projects/blob/master/Data_Challenges/Geolocalisation/output.png)

## Materials

For this challenge, we have:
- Training samples and their corresponding predictions
- Test samples for which we need to produce predictions associated

## Project structure
<!-- TOC -->
- [LOAD DATA](#load-data)
- [DATA EXPLORATION](#data-exploration)
   - [Map](#map)
   - [Distribution](#distribution)
- [PREPROCESSING](#preprocessing)
- [PREDICTION](#prediction)
   - [Linear regression](#linear-regression)
      - [Cross validation](#cross-validation)
   - [Random forests](#random-forests)
      - [Cross validation Leave One Device Out](#cross-validation---leave-one-device-out)	
- [POSTPROCESSING](#postprocessing)
<!-- /TOC -->

## Load data

Three dataframes are used:
- training samples
- their corresponding predictions
- test samples

## Data exploration

### Map
We first display device (from message data) and bases on a map. We notice some bases are very far away from the device, which are outliers.

We then decide to display the same map without outliers. Arbitrary distances have been choosen: we keep latitude between 43 and 65 and longitudes between -65 and -104.

### Distribution
Distributions are plotted to get a view on distances repartitions. Function _dist__calculation_ is used to compute the distance based on latitude and longitude.

## Preprocessing
We remove outliers from the train set. After several tests, it looked that removing device further than 10 kms gives much better results. Besides, it is reasonable to include device only with small distances to the base.

We then build the feature matrice. In rows there are the message IDs. In columns, all the bases from the train set. Those are repeated 3 times: for rssi, for base latitude and base longitude.

The associated predicted values need to have the same format. Thus, we need to group by message id (_messid_) the latitude and longitude, using the mean.

## Prediction

### Linear regression

#### "Cross validation"
We use the sickit-learn function [cross_val_predict](https://scikit-learn.org/stable/modules/generated/sklearn.model_selection.cross_val_predict.html) to predict latitude and longitude. This function takes in parameter the number of folds _cv_. Using _cross_val_predict_: "For each element in the input, the prediction that was obtained for that element when it was in the test set". As explained, it is <span style="text-decoration: underline">not</span> a scoring function, just several predictions based on changing samples.

Suppose we have the following training set:

[1] X1 -> Y1
[2] X2 -> Y2
[3] X3 -> Y3
[4] X4 -> Y4

We now perform _cross_val_predict(cv=2)_

[1] X1 -> Y1 (training)
[2] X2 -> Y2 (training)
[3] X3 -> Y3' (prediction)
[4] X4 -> Y4' (prediction)

[1] X1 -> Y1' (prediction)
[2] X2 -> Y2' (prediction)
[3] X3 -> Y3 (training)
[4] X4 -> Y4 (training)

At the end we have [Y1', Y2', Y3', Y4']

It appears that linear regression gives outliers (latitude <-90 or >90). We thus remove those corrupted data:

```grgffef
```

### Random forests

#### Cross validation Leave One Device Out


