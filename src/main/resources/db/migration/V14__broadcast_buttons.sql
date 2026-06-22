ALTER TABLE broadcasts ADD COLUMN buttons JSONB;

UPDATE broadcasts
SET buttons = jsonb_build_array(jsonb_build_array(jsonb_build_object('text', button_text, 'url', button_url)))
WHERE button_text IS NOT NULL AND button_url IS NOT NULL;

ALTER TABLE broadcasts DROP COLUMN button_text;
ALTER TABLE broadcasts DROP COLUMN button_url;
