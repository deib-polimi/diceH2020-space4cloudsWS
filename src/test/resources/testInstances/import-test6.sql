DELETE FROM TYPEVM;
DELETE FROM PROVIDER;
INSERT INTO PROVIDER(p_id) VALUES('Amazon');
INSERT INTO PROVIDER(p_id) VALUES('Flexi');
INSERT INTO TYPEVM(core, deltabar, p_id, rhobar,sigmabar,type) VALUES(1,2.1, 'Amazon',0.9,0.4,'small');
INSERT INTO TYPEVM(core, deltabar, p_id, rhobar,sigmabar,type) VALUES(2,1.4, 'Amazon',1.1,0.3,'medium');
INSERT INTO TYPEVM(core, deltabar, p_id, rhobar,sigmabar,type) VALUES(4,1.2, 'Amazon',1.2,0.2,'large');
INSERT INTO TYPEVM(core, deltabar, p_id, rhobar,sigmabar,type) VALUES(4,2.0, 'Flexi',1.4,0.5,'T3');
