# Geolocalisation Data Challenge


## Objective

A company that produces connected device needs to locate the position of their device. They could use GPS but using prediction based on data seems cheaper.
Prediction is based on the message reception information.

## Context
This project was part of the curriculum of our Post-Master's Degree in Big Data at Telecom Paris.
Full curriculum and details on this Degree [here](
https://www.telecom-paris.fr/en/post-masters-degree/all-post-masters-degree/post-masters-degree-in-big-data "here").

## Team Members
Name  | Github
------------- | -------------
Gaël Savouré | [savoga](http://github.com/savoga "savoga")
Thomas Rivière | [t-riviere](http://github.com/t-riviere "t-riviere")
Hiroto Yamakakawa | [yamhiroto](http://github.com/yamhiroto "yamhiroto")

## Input

Inputs are different information about messages:

![InputImage](https://github.com/savoga/various_projects/blob/master/Data_Challenges/Geolocalisation/Inputs.png)

Note: _rssi_ (Received Signal Strength Indicator) is an estimation of the signal power level received by the device.

Note: we won't use column _nseq_ and _time_ux_.

## Output

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
      - [Performance measure](#performance-measure)
   - [Random forests](#random-forests)
      - [Cross validation Leave One Device Out](#cross-validation-leave-one-device-out)	
      - [Performance measure (2)](#performance-measure-(2))
- [POSTPROCESSING](#postprocessing)
- [LIMITS AND IMPROVEMENTS](#limits-and-improvements)
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
We use the sickit-learn function [cross_val_predict](https://scikit-learn.org/stable/modules/generated/sklearn.model_selection.cross_val_predict.html) to predict latitude and longitude. This function takes in parameter the number of folds _cv_. Using _cross_val_predict_: "For each element in the input, the prediction that was obtained for that element when it was in the test set". As explained, it is **not** a scoring function, just several predictions based on changing samples.

Suppose we have the following training set:

[1] X1 -> Y1

[2] X2 -> Y2

[3] X3 -> Y3

[4] X4 -> Y4

We now perform _cross_val_predict(cv=2)_

1)

[1] X1 -> Y1 (training)

[2] X2 -> Y2 (training)

[3] X3 -> Y3' (prediction)

[4] X4 -> Y4' (prediction)

2)

[1] X1 -> Y1' (prediction)

[2] X2 -> Y2' (prediction)

[3] X3 -> Y3 (training)

[4] X4 -> Y4 (training)

At the end we have [Y1', Y2', Y3', Y4']

It appears that linear regression gives outliers (latitude <-90 or >90). We thus remove those corrupted data:

`indexes_to_remove = np.where((y_pred_lat > 90) | (y_pred_lat < -90))[0]`

#### Performance measure

Next we plot the cumulative probabilities. This is simply the cumulative sum of errors divided by the sum of errors:

``plt.plot(base[:-1]/1000, cumulative / np.float(np.sum(values))  * 100.0, c='blue')``

We look at the error of the 80th percentile, that is around 7.5 kms on the figure.

<img src="https://github.com/savoga/various_projects/blob/master/Data_Challenges/Geolocalisation/cumsum.png" width="500" height="300">

### Random forests

#### Cross validation Leave One Device Out

Leave One Device Out strategy consists in splitting the whole train set into unique device. It allows to make the training and the prediction on distinct device.

#### Performance measure (2)

Performance measure is the same as for linear regression, but we do this multiple time (in fact, the number of device) and take the mean as a final score.

We note that Random Forests gave significant better results than linear regression.

## Postprocessing

Training bases and test bases are different, we thus can't build the same matrice of features as above (columns will be different).
We decided to use the same structure as for the training phase, that is using the same bases. Our rationale behind this is that we can't predict using new bases as we never trained on them and don't know their signal reliability. Note that after building the structure, the double loop can take some time.

## Limits and Improvements

We arbitrarly removed outliers that gave bad scores to our models. However, outliers can be present in the test sample and make the prediction bad.

We could think about several other models, especially XGBoost that usually gives good results.
