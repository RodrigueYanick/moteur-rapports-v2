CREATE TABLE report_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE report_variables (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    default_value VARCHAR(255),
    required BOOLEAN NOT NULL,
    template_id UUID,
    CONSTRAINT fk_report_variables_template FOREIGN KEY (template_id) REFERENCES report_templates(id)
);

CREATE TABLE report_generations (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    format VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
