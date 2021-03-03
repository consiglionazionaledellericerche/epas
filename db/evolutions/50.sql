# ---!Ups

CREATE TABLE contract_month_recap
(
  id BIGSERIAL PRIMARY KEY,
  year INTEGER,
  month INTEGER,
  
  -- modulo recap assenze
  abs_fap_usate INTEGER,
  abs_fac_usate INTEGER,
  abs_p_usati INTEGER,
  abs_rc_usati INTEGER,

  -- modulo recap residui
  -- fonti dell'algoritmo
  s_r_bp INTEGER,
  s_bp_consegnati INTEGER,
  s_bd_usati INTEGER,
  s_r_ac_initmese INTEGER,
  s_r_ap INTEGER,
  s_r_ac INTEGER,
  s_pf INTEGER,
  s_pfp INTEGER,
  s_r_ap_usabile BOOLEAN,
  s_s1 INTEGER,
  s_s2 INTEGER,
  s_s3 INTEGER,
  s_rc_min INTEGER,
  s_ol INTEGER,
  
  -- decisioni dell'algoritmo imputazioni
  d_pfn_ap INTEGER,
  d_pfn_ac INTEGER,
  d_pfn_pfp INTEGER,
  d_rc_ap INTEGER,
  d_rc_ac INTEGER,
  d_rc_pfp INTEGER,
  
  -- decisioni dell'algoritmo residui finali
  d_r_ap INTEGER,	
  d_r_ac INTEGER,	
  d_r_bp INTEGER,	

  contract_id bigint NOT NULL,
  
  CONSTRAINT contract_contract_month_recap FOREIGN KEY (contract_id)
      REFERENCES contracts (id) 
);

# ---!Downs

DROP TABLE contract_month_recap;

