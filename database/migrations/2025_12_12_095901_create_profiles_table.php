<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::create('profiles', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('avatar')->nullable();
            $table->string('bio')->nullable();
            $table->string('location')->nullable();
            $table->string('website')->nullable();
            $table->string('twitter')->nullable();
            $table->string('github')->nullable();
            $table->string('linkedin')->nullable();
            $table->string('instagram')->nullable();
            $table->string('facebook')->nullable();
            $table->string('youtube')->nullable();
            $table->string('discord')->nullable();
            $table->string('phone')->nullable();
            $table->string('birthday')->nullable();
            $table->string('gender')->nullable();
            $table->text('description')->nullable();
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('profiles');
    }
};
