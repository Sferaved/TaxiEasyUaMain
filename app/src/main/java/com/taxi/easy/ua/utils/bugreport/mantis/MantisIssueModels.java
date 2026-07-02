package com.taxi.easy.ua.utils.bugreport.mantis;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class MantisIssueModels {

    private MantisIssueModels() {
    }

    public static class CreateIssueRequest {
        public String summary;
        public String description;
        public IdRef project;
        public IdRef category;
        public List<FileAttachment> files;
    }

    public static class IdRef {
        public int id;

        public IdRef(int id) {
            this.id = id;
        }
    }

    public static class FileAttachment {
        public String name;
        public String content;

        public FileAttachment(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }

    public static class CreateIssueResponse {
        public IssueRef issue;
    }

    public static class IssueRef {
        public int id;
        @SerializedName("summary")
        public String summary;
    }
}
