package com.hybridsearch.model

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("fineweb_data")
@Document(indexName = "fineweb_data")
data class FinewebData(
    @Id
    @Field(type = FieldType.Keyword)
    val id: UUID,

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    val text: String,

    @Field(type = FieldType.Object)
    val metadata: Map<String, Any>?,


)

