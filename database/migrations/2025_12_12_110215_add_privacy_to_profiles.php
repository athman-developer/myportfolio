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
        Schema::table('profiles', function (Blueprint $table) {
            $table->enum('profile_visibility', ['public', 'private'])->default('public')->after('description');
            $table->boolean('show_email')->default(true)->after('profile_visibility');
            $table->boolean('show_phone')->default(false)->after('show_email');
            $table->boolean('show_location')->default(true)->after('show_phone');
            $table->boolean('show_social_links')->default(true)->after('show_location');
            $table->boolean('allow_messages')->default(true)->after('show_social_links');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('profiles', function (Blueprint $table) {
            //
        });
    }
};
