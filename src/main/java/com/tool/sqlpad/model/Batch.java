package com.tool.sqlpad.model;

import lombok.Data;

import java.util.List;

@Data
public class Batch {
    private String id;
    private String connectionId;
    private List<Statement> statements;
    @Data
    public static class Statement {
        private String id;
        private String batchId;
        private List<Column> columns;
    }

    @Data
    public static class Column {
        private String datatype;
        private String name;
    }
}
