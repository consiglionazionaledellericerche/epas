# --- !Ups

-- Creo una nouva revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

CREATE TABLE contract_month_recap_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
	year INTEGER,
    month INTEGER,
  	abs_rc_usati INTEGER,
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
	d_pfn_ap INTEGER,
	d_pfn_ac INTEGER,
	d_pfn_pfp INTEGER,
	d_rc_ap INTEGER,
	d_rc_ac INTEGER,
	d_rc_pfp INTEGER,
	d_r_ap INTEGER,	
	d_r_ac INTEGER,	
	d_r_bp INTEGER,	
	s_r_bp_init INTEGER,
	d_91ce_ap INTEGER,
	d_91ce_ac INTEGER,
	d_91ce_pfp INTEGER,
	s_91ce_min INTEGER,
	contract_id bigint NOT NULL
);

INSERT INTO contract_month_recap_history (id, _revision, _revision_type, year, month, abs_rc_usati, 
	s_r_bp, s_bp_consegnati, s_bd_usati, s_r_ac_initmese, s_r_ap, s_r_ac, s_pf, s_pfp, s_r_ap_usabile,
	s_s1, s_s2, s_s3, s_rc_min, s_ol, d_pfn_ap, d_pfn_ac, d_pfn_pfp, d_rc_ap, d_rc_ac, d_rc_pfp, d_r_ap, d_r_ac, d_r_bp,
	s_r_bp_init, d_91ce_ap, d_91ce_ac, d_91ce_pfp, s_91ce_min, contract_id) 
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, year, month, abs_rc_usati, s_r_bp, s_bp_consegnati, 
	s_bd_usati, s_r_ac_initmese, s_r_ap, s_r_ac, s_pf, s_pfp, s_r_ap_usabile, s_s1, s_s2, s_s3, s_rc_min, s_ol, 
	d_pfn_ap, d_pfn_ac, d_pfn_pfp, d_rc_ap, d_rc_ac, d_rc_pfp, d_r_ap, d_r_ac, d_r_bp, s_r_bp_init, d_91ce_ap, d_91ce_ac, 
	d_91ce_pfp, s_91ce_min, contract_id 
	FROM contract_month_recap;

-- Non è necessaria una down