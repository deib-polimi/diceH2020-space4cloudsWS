INSERT INTO PROVIDER(p_id) VALUES('Amazon');
INSERT INTO PROVIDER(p_id) VALUES('Cineca');
INSERT INTO TYPEVM(core,memory,deltabar,p_id,rhobar,sigmabar,type) VALUES(1,4,0.07,'Amazon',0.051,0.012,'medium');
INSERT INTO TYPEVM(core,memory,deltabar,p_id,rhobar,sigmabar,type) VALUES(2,8,0.13,'Amazon',0.11,0.016,'large');
INSERT INTO TYPEVM(core,memory,deltabar,p_id,rhobar,sigmabar,type) VALUES(4,16,0.25,'Amazon',0.225,0.03,'xlarge');
INSERT INTO TYPEVM(core,memory,deltabar,p_id,rhobar,sigmabar,type) VALUES(8,32,0.51,'Amazon',0.44,0.071,'2xlarge');
INSERT INTO TYPEVM(core,memory,deltabar,p_id,rhobar,sigmabar,type) VALUES(20,120,1.263,'Cineca',1.09,0.1875,'5xlarge');