-- V5：学员家长称呼，便于教务代录与家长端展示
ALTER TABLE students ADD COLUMN parent_name VARCHAR(64) NULL AFTER parent_phone;

UPDATE students s
    LEFT JOIN phone_whitelist w ON w.org_id = s.org_id AND w.phone = s.parent_phone
SET s.parent_name = COALESCE(NULLIF(w.parent_name, ''), NULL)
WHERE s.parent_name IS NULL;

UPDATE students SET parent_name = '家长' WHERE parent_name IS NULL OR parent_name = '';
