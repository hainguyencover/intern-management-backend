package com.example.backend.elasticsearch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "interns")
public class InternIndex {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String fullName;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Keyword)
    private String studentCode;

    @Field(type = FieldType.Text)
    private String university;

    @Field(type = FieldType.Text)
    private String major;

    @Field(type = FieldType.Double)
    private Double gpa;
}
