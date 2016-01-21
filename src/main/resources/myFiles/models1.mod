# vim: set filetype=ampl : #
#################################################
### YARN models                               ###
### liliane tueguem 12/06/2015                ###
#################################################

### Sets
param nAM integer >=0;
set AM := 1..nAM;

### Parameters
param Gamma integer >=0;
param rhobar {i in AM}>=0;
param deltabar{i in AM}>=0;
param sigmabar{i in AM}>=0;

param HUp {i in AM} integer >=0;
param HLow {i in AM} integer >=0;
param gammaLow {i in AM} >=0;
param gammaUp {i in AM} >=0;
param psiLow {i in AM} >=0;
param psiUp {i in AM} >=0;

param cM {i in AM} integer >=0;
param cR {i in AM} integer >=0;
param R {i in AM} integer >=0;
param alpha {i in AM} >=0;
param beta {i in AM} >=0;
param job_penalty {i in AM} >=0;
param eta {i in AM} >= 0;

### Job profile
param A {i in AM} >=0;
param B {i in AM} >=0;
param E {i in AM} <=0;
param NM {i in AM} integer >=0;
param NR {i in AM} integer >=0;
param Mavg {i in AM} >=0;
param Mmax {i in AM} >=0;
param Ravg {i in AM} >=0;
param Rmax {i in AM} >=0;
param w {i in AM} >=0;
param C {i in AM} >=0;
param D {i in AM} >=0;
param SH1max {i in AM} >=0;
param SHtypmax {i in AM} >=0;
param SHtypavg {i in AM} >=0;

param xiM {i in AM} >=0;
param xiR {i in AM} >=0;
param K {i in AM} >=0;

### Variables
var r {i in AM} >=0;
var d {i in AM} >=0;
var s {i in AM} >=0;
var psi {i in AM} >=0;
var gamma {i in AM} >=0;
var sM {i in AM} >=0;
var sR {i in AM} >=0;


### Constraints
s.t. min_resources {i in AM}: gamma[i] >= gammaLow[i];
s.t. max_resources {i in AM}: gamma[i] <= gammaUp[i];

s.t. reserved_capacity {i in AM}: r[i] <= R[i];

s.t. capacity: sum {i in AM} (w[i] * gamma[i]) <= Gamma;

s.t. reserved_bound {i in AM}: r[i] <= (1 - eta[i]) * gamma[i];
s.t. other {i in AM}: s[i] <= (eta[i] / (1 - eta[i])) * (r[i] + d[i]);

### Objectives
minimize centralized_obj: sum {i in AM} (rhobar[i] * r[i] + deltabar[i] * ((1 - eta[i]) * gamma[i] - r[i]) + sigmabar[i] * eta[i] * gamma[i] + alpha[i] * K[i] / gamma[i] - beta[i]);
