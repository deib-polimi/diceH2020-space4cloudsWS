MERGE INTO PROVIDER(p_id) VALUES('Amazon');
MERGE INTO PROVIDER(p_id) VALUES('Cineca');

INSERT INTO TYPEVM(cores, memory, delta_bar, p_id, rho_bar, sigma_bar, type)
  SELECT * FROM
    VALUES(1, 4, 0.07, 'Amazon', 0.051, 0.012, 'medium')
  WHERE NOT EXISTS (SELECT p_id, type FROM TYPEVM WHERE p_id = 'Amazon' AND type = 'medium');

INSERT INTO TYPEVM(cores, memory, delta_bar, p_id, rho_bar, sigma_bar, type)
  SELECT * FROM
    VALUES(2, 8, 0.13, 'Amazon', 0.11, 0.016, 'large')
  WHERE NOT EXISTS (SELECT p_id, type FROM TYPEVM WHERE p_id = 'Amazon' AND type = 'large');

INSERT INTO TYPEVM(cores, memory, delta_bar, p_id, rho_bar, sigma_bar, type)
  SELECT * FROM
    VALUES(4, 16, 0.25, 'Amazon', 0.225, 0.03, 'xlarge')
  WHERE NOT EXISTS (SELECT p_id, type FROM TYPEVM WHERE p_id = 'Amazon' AND type = 'xlarge');

INSERT INTO TYPEVM(cores, memory, delta_bar, p_id, rho_bar, sigma_bar, type)
  SELECT * FROM
    VALUES(8, 32, 0.51, 'Amazon', 0.44, 0.071, '2xlarge')
  WHERE NOT EXISTS (SELECT p_id, type FROM TYPEVM WHERE p_id = 'Amazon' AND type = '2xlarge');

INSERT INTO TYPEVM(cores, memory, delta_bar, p_id, rho_bar, sigma_bar, type)
  SELECT * FROM
    VALUES(20, 120, 1.263, 'Cineca', 1.09, 0.1875, '5xlarge')
  WHERE NOT EXISTS (SELECT p_id, type FROM TYPEVM WHERE p_id = 'Cineca' AND type = '5xlarge');
