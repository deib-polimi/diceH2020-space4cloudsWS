---
-- Copyright 2017 Eugenio Gianniti
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
---

-- Model specification

-- DAG definition: it is encoded as an array of stages.
Stages = @@STAGES@@;

-- Number of computation nodes in the system
Containers = @@CONTAINERS@@;

-- Number of users accessing the system
Users = @@USERS@@;

-- Distribution of the think time for the users
UThinkTimeDistr = @@THINK_PDF@@;

-- Total number of jobs to simulate
maxJobs = @@MAXJOBS@@;

-- Coefficient for the Confidence Intervals
-- 99%	2.576
-- 98%	2.326
-- 95%	1.96
-- 90%	1.645
confIntCoeff = @@QUANTILE@@;
